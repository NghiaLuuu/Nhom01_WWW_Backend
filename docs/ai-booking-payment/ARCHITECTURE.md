# KIẾN TRÚC HỆ THỐNG AI BOOKING & DEMO PAYMENT

## 1. SƠ ĐỒ KIẾN TRÚC TỔNG THỂ

```
+-----------------------------------------------------------------------------------+
|                                FRONTEND (React + Vite)                            |
|  +---------------------+  +----------------------------------------------------+  |
|  | ChatbotWidget.tsx   |  | Interactive Chat Cards                             |  |
|  | - Chat Interface    |  | [TripList] [SeatSelection] [ConfirmationCard]      |  |
|  | - Live 5m Countdown |  | [DemoPaymentQRCard] [TicketSuccessCard]            |  |
|  +----------+----------+  +-------------------------+--------------------------+  |
+-------------|---------------------------------------|-----------------------------+
              | HTTPS REST API (JWT Bearer Token)     |
              v                                       v
+-----------------------------------------------------------------------------------+
|                        BACKEND (Spring Boot 4.1.1 + Spring AI)                    |
|                                                                                   |
|  +-----------------------------+       +---------------------------------------+  |
|  | ChatbotController           |       | DemoPaymentController                 |  |
|  | /api/chatbot/booking-assist |       | /api/payments/demo/create-qr          |  |
|  | /api/chatbot/ask            |       | /api/payments/demo/confirm            |  |
|  +--------------+--------------+       +-------------------+-------------------+  |
|                 |                                          |                      |
|                 v                                          v                      |
|  +-----------------------------+       +---------------------------------------+  |
|  | BookingAssistantService     |       | DemoPaymentProcessor                  |  |
|  | - Intent Parser (NLP)       |       | - Guard: app.demo-payment.enabled     |  |
|  | - Spring AI Tool Calling    |       | - Ownership & TTL Expiry Validation   |  |
|  | - State Orchestration       |       | - Status: PENDING -> SIMULATED_PAID   |  |
|  +--------------+--------------+       +-------------------+-------------------+  |
|                 |                                          |                      |
|                 v                                          |                      |
|  +-----------------------------+                           |                      |
|  | BookingTools                |                           |                      |
|  | - searchAvailableTrips      |                           |                      |
|  | - suggestAdjacentSeats      |                           |                      |
|  | - prepareBooking / confirm  |                           |                      |
|  +-------+-------------+-------+                           |                      |
|          |             |                                   |                      |
|          v             v                                   v                      |
|  +---------------+  +------------------+  +------------------------------------+  |
|  | VectorSearch  |  | BookingService   |  | TicketService / QRCodeGenerator    |  |
|  | (pgvector)    |  | & SeatHoldRedis  |  | - Generates Base64 QR              |  |
|  +-------+-------+  +--------+---------+  +------------------+-----------------+  |
+----------|-------------------|-------------------------------|--------------------+
           v                   v                               v
+-----------------------------------------------------------------------------------+
|                       DATA LAYER & INFRASTRUCTURE                                 |
|  +----------------------------+   +-------------------+   +--------------------+  |
|  | PostgreSQL (Relational DB) |   | pgvector (HNSW)   |   | Redis (Seat Hold)  |  |
|  | - Booking (PENDING -> PAID)|   | - Semantic Route  |   | - 5-min Hold Lock  |  |
|  | - Payment (SIMULATED_PAID) |   |   Metadata Index  |   | - Concurrency Guard|  |
|  | - Ticket (ISSUED)          |   |                   |   |                    |  |
|  +----------------------------+   +-------------------+   +--------------------+  |
+-----------------------------------------------------------------------------------+
```

---

## 2. THIẾT KẾ PHÂN BIỆT 2 LOẠI MÃ QR

Hệ thống tuân thủ nghiêm ngặt nguyên tắc **độc lập và chuyên biệt** giữa thanh toán mô phỏng và vé điện tử:

### 2.1 Mã QR Thanh toán Mô phỏng (Demo Payment QR)
- **Mục đích:** Mô phỏng quét mã thanh toán cho giao dịch demo, kiểm thử chức năng đồ án.
- **Payload cấu trúc:**
  ```text
  DEMO_PAYMENT|BOOKING:501|TXN:DEMO_TXN_501_1793613240|AMOUNT:500000|STATUS:PENDING|NOTICE:INTERNAL_DEMO_NO_REAL_MONEY
  ```
- **Đặc điểm nhận dạng:**
  - Tiền tố `DEMO_PAYMENT`
  - Chứa cờ cảnh báo `NOTICE:INTERNAL_DEMO_NO_REAL_MONEY`
  - Tuyệt đối **không giả dạng VietQR, NAPAS, MoMo** hay tài khoản ngân hàng thực tế.
  - Hết hạn theo thời hạn giữ chỗ 5 phút của Redis.

### 2.2 Mã QR Vé điện tử (Electronic Ticket QR)
- **Mục đích:** Sử dụng khi lên xe, tài xế/soát vé dùng ứng dụng quét mã để Check-in.
- **Payload cấu trúc:**
  ```text
  TICKET:VXDEMO1793613240701|BOOKING:501|SEAT:A01|PRICE:250000
  ```
- **Đặc điểm nhận dạng:**
  - Tiền tố `TICKET:`
  - Mỗi hành khách / mỗi ghế có một mã QR độc lập.
  - Tồn tại vĩnh viễn cùng vé điện tử (trạng thái `ISSUED`), không bị xóa sau khi thanh toán.

---

## 3. CƠ CHẾ GIAO DỊCH, KHÓA GHẾ & IDEMPOTENCY

1. **Khóa giữ chỗ (Seat Hold Locking):**
   - Khi đơn đặt vé được tạo (`Booking.status = PENDING_PAYMENT`), `SeatHoldRedisService` giữ các mã ghế tương ứng với TTL = 300 giây (5 phút).
   - Nếu khách hàng khác cùng chọn ghế đó trong khoảng thời gian này, hệ thống từ chối với thông báo ghế đang được giữ chỗ.
2. **Xác nhận thanh toán mô phỏng nguyên tử (`@Transactional`):**
   - Cập nhật `Payment.status = SIMULATED_PAID`
   - Cập nhật `Booking.status = PAID`
   - Cập nhật các `Ticket.status = ISSUED`
   - Giải phóng khóa tạm thời trên Redis (`releaseHoldByBookingId`).
3. **Idempotency (Chống thanh toán trùng lặp khi retry):**
   - Nếu người dùng hoặc mạng nhấn "Mô phỏng thanh toán" 2 lần:
   - `DemoPaymentProcessor` kiểm tra nếu `booking.getStatus().equals("PAID")`, hệ thống lập tức trả về thông tin vé đã phát hành trước đó, **không cập nhật lại DB và không tạo payment mới**.
