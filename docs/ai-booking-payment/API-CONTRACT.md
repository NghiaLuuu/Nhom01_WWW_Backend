# HỢP ĐỒNG API & DỮ LIỆU (API CONTRACT)

Tài liệu này cung cấp chi tiết kỹ thuật cho các API phục vụ AI Booking và Thanh toán Demo trong hệ thống VEXE.

---

## 1. ENDPOINT: HỘI THOẠI TRỢ LÝ ĐẶT VÉ AI

- **URL:** `POST /api/chatbot/booking-assistant` (hoặc `POST /api/chatbot/ask`)
- **Phân quyền:** Công khai (Anonymous tra cứu được, nhưng xác nhận & thanh toán cần Bearer Token).
- **Headers:**
  - `Content-Type: application/json`
  - `Authorization: Bearer <jwt_token>` (nếu đã đăng nhập)

### 1.1 Request Payload (`ChatbotRequest`)
```json
{
  "question": "Đặt giúp tôi 2 vé đi Đà Lạt tối mai",
  "history": [
    {
      "role": "user",
      "content": "Xin chào"
    },
    {
      "role": "assistant",
      "content": "Chào bạn! Tôi có thể giúp gì cho bạn?"
    }
  ]
}
```

### 1.2 Response Payload (`ApiResponse<Map<String, Object>>`)

#### a. Trường hợp thiếu thông tin điểm đi (`status: "NEED_INFO"`):
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "status": "NEED_INFO",
    "answer": "Dạ anh/chị muốn khởi hành từ điểm nào đến Đà Lạt ạ? (Ví dụ: Sài Gòn, Nha Trang...)"
  }
}
```

#### b. Trường hợp hiển thị chuyến xe (`cardType: "TRIP_LIST"`):
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "status": "TRIP_LIST",
    "cardType": "TRIP_LIST",
    "answer": "Em đã tìm thấy các chuyến xe phù hợp...",
    "trips": [
      {
        "tripId": 1001,
        "routeName": "TP. Hồ Chí Minh -> Đà Lạt",
        "departureLocation": "TP. Hồ Chí Minh",
        "arrivalLocation": "Đà Lạt",
        "departureTimeFormatted": "21:00 ngày 03/11/2026",
        "priceFormatted": "250.000 VNĐ",
        "vehicleType": "Limousine",
        "availableSeats": 34
      }
    ]
  }
}
```

#### c. Trường hợp hỏi chọn ghế cạnh nhau (`cardType: "SEAT_SELECTION"`):
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "status": "SEAT_SELECTION",
    "cardType": "SEAT_SELECTION",
    "tripId": 1001,
    "availableSeatsCount": 34,
    "answer": "🚌 Anh/chị đã chọn chuyến xe #1001. Bạn có muốn chọn 2 ghế cạnh nhau không?"
  }
}
```

#### d. Trường hợp tóm tắt đặt vé chờ xác nhận (`cardType: "BOOKING_CONFIRMATION"`):
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "status": "CONFIRMATION_REQUIRED",
    "cardType": "BOOKING_CONFIRMATION",
    "answer": "TÓM TẮT ĐƠN ĐẶT VÉ CHỜ XÁC NHẬN...",
    "draft": {
      "idempotencyKey": "a1fc39da-6fc6-45ff-8b6a-fe84b11d51e1",
      "tripId": 1001,
      "routeName": "TP. Hồ Chí Minh -> Đà Lạt",
      "departureTimeFormatted": "21:00 ngày 03/11/2026",
      "vehicleType": "Limousine",
      "seatNames": ["A01", "A02"],
      "totalPriceFormatted": "500.000 VNĐ",
      "summaryMessage": "..."
    }
  }
}
```

#### e. Trường hợp tạo Booking & QR Demo thành công (`cardType: "DEMO_PAYMENT_QR"`):
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "status": "BOOKING_CREATED",
    "cardType": "DEMO_PAYMENT_QR",
    "bookingId": 501,
    "bookingCode": "BK501DEMO",
    "answer": "🎉 ĐẶT VÉ THÀNH CÔNG! Mã đặt vé: BK501DEMO...",
    "payment": {
      "bookingId": 501,
      "bookingCode": "BK501DEMO",
      "transactionId": "DEMO_TXN_501_1793613240",
      "amount": 500000,
      "amountFormatted": "500.000 VNĐ",
      "qrBase64": "data:image/png;base64,iVBORw0KGgo...",
      "qrPayload": "DEMO_PAYMENT|BOOKING:501|TXN:DEMO_TXN_501_1793613240|AMOUNT:500000|STATUS:PENDING|NOTICE:INTERNAL_DEMO_NO_REAL_MONEY",
      "expiresInSeconds": 300,
      "demoNote": "THANH TOÁN MÔ PHỎNG — KHÔNG CHUYỂN TIỀN THẬT"
    }
  }
}
```

#### f. Trường hợp thanh toán mô phỏng thành công (`cardType: "TICKET_SUCCESS"`):
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "status": "PAYMENT_SUCCESS",
    "cardType": "TICKET_SUCCESS",
    "bookingId": 501,
    "bookingCode": "BK501DEMO",
    "answer": "🎉 THANH TOÁN MÔ PHỎNG THÀNH CÔNG!...",
    "ticket": {
      "success": true,
      "bookingId": 501,
      "bookingCode": "BK501DEMO",
      "status": "PAID",
      "totalPrice": 500000,
      "totalPriceFormatted": "500.000 VNĐ",
      "paymentMethod": "DEMO_SIMULATION",
      "transactionId": "DEMO_TXN_501_1793613240",
      "routeName": "TP. Hồ Chí Minh -> Đà Lạt",
      "departureTimeFormatted": "21:00 ngày 03/11/2026",
      "tickets": [
        {
          "ticketId": 701,
          "ticketCode": "VXDEMO1793613240701",
          "seatName": "A01",
          "floor": 1,
          "price": 250000,
          "priceFormatted": "250.000 VNĐ",
          "status": "ISSUED",
          "qrPayload": "TICKET:VXDEMO1793613240701|BOOKING:501|SEAT:A01|PRICE:250000",
          "qrCodeBase64": "data:image/png;base64,..."
        },
        {
          "ticketId": 702,
          "ticketCode": "VXDEMO1793613240702",
          "seatName": "A02",
          "floor": 1,
          "price": 250000,
          "priceFormatted": "250.000 VNĐ",
          "status": "ISSUED",
          "qrPayload": "TICKET:VXDEMO1793613240702|BOOKING:501|SEAT:A02|PRICE:250000",
          "qrCodeBase64": "data:image/png;base64,..."
        }
      ]
    }
  }
}
```

---

## 2. ENDPOINT: QUẢN LÝ THANH TOÁN MÔ PHỎNG TRỰC TIẾP

Dành cho frontend khi gọi trực tiếp qua service:

### 2.1 Sinh mã QR Demo
- **URL:** `POST /api/payments/demo/create-qr`
- **Body:** `{ "bookingId": 501 }`
- **Response:** `DemoPaymentQrResponse`

### 2.2 Xác nhận thanh toán mô phỏng
- **URL:** `POST /api/payments/demo/confirm`
- **Body:**
  ```json
  {
    "bookingId": 501,
    "transactionId": "DEMO_TXN_501_1793613240"
  }
  ```
- **Response:** `DemoPaymentConfirmResponse`

### 2.3 Hủy thanh toán / Đơn giữ chỗ
- **URL:** `POST /api/payments/demo/cancel/{bookingId}`
- **Response:** `{ "success": true, "message": "Đã hủy đơn đặt vé thành công" }`

---

## 3. BẢNG MÃ LỖI THƯỜNG GẶP (HTTP STATUS CODES)

| Mã lỗi | Nguyên nhân | Ý nghĩa / Hướng xử lý |
|---|---|---|
| `400 BAD_REQUEST` | Tham số không hợp lệ hoặc trạng thái booking không phải `PENDING_PAYMENT` | Kiểm tra lại dữ liệu đầu vào |
| `401 UNAUTHORIZED` | Khách hàng chưa đăng nhập khi xác nhận đặt vé hoặc thanh toán | Chuyển hướng đăng nhập |
| `403 FORBIDDEN` | Tài khoản cố thao tác trên booking của người khác, HOẶC `app.demo-payment.enabled = false` | Bảo vệ dữ liệu & Production Guard |
| `404 NOT_FOUND` | Không tìm thấy chuyến xe hoặc đơn đặt vé tương ứng | Kiểm tra mã chuyến / mã đơn |
| `410 GONE` | Đơn giữ chỗ quá 5 phút đã bị hết hạn và ghế đã bị thu hồi | Yêu cầu khách đặt lại vé |
| `409 CONFLICT` | Ghế đã có người khác chọn hoặc giữ chỗ trong Redis | Chọn lại ghế khác |
