# BÁO CÁO KỸ THUẬT: KHẮC PHỤC TOÀN DIỆN LUỒNG AI BOOKING, STATE MACHINE & PAYMENT

## 1. TỔNG QUAN HỆ THỐNG & MỤC TIÊU
Tài liệu này tổng hợp toàn bộ phân tích nguyên nhân gốc rễ (Root Cause Analysis), giải pháp thiết kế State Machine Backend, thuật toán sắp xếp sơ đồ ghế tự nhiên (Natural Alphanumeric Seat Sorting & Adjacency) và kết quả nghiệm thu kiểm thử cho 8 sự cố được phát hiện trong luồng đặt vé trợ lý AI chatbot.

---

## 2. PHÂN TÍCH NGUYÊN NHÂN GỐC RỄ & GIẢI PHÁP 8 LỖI

| STT | Sự cố thực tế | Nguyên nhân gốc rễ (Root Cause) | Giải pháp kỹ thuật đã triển khai | Trạng thái |
|:---:|:---|:---|:---|:---:|
| **1** | Đặt 2 vé nhưng hệ thống chỉ tạo 1 booking ticket | `BookingIntentParser` reset `passengerCount = 1` ở các lượt chat tiếp theo (như "Có", "Chọn chuyến", "Xác nhận") do không kế thừa số lượng khách từ lịch sử hội thoại. | Bổ sung cơ chế `inheritPassengerCount` quét ngược context và lưu trữ `passengerCount` vào `BookingSessionState` do Backend quản lý. Giữ vững `passengerCount=2` xuyên suốt. | **PASS** |
| **2** | Chọn chuyến 20:45 nhưng AI tự chuyển sang chuyến 06:30 | Khi người dùng chat "tôi chọn chuyến 20:45", hệ thống fallback auto-pick `trips.get(0)` (chuyến sáng sớm 06:30) vì không parse được giờ tự nhiên thành `targetTripTime`. | Bổ sung `NATURAL_TRIP_TIME_PATTERN` nhận diện "20:45", "chuyến lúc 20h45", đối chiếu danh sách chuyến và khóa `selectedTripId` vào `BookingSessionState`. | **PASS** |
| **3** | AI hiển thị Booking Confirmation Card quá sớm | Workflow deterministic kích hoạt `prepareBooking` ngay từ turn 1 khi vừa tìm thấy chuyến, bỏ qua các bước chọn chuyến và chọn ghế. | Xây dựng State Machine tuần tự 7 bước: `COLLECTING_INFO` → `TRIP_SELECTION` → `SEAT_SELECTION` → `BOOKING_REVIEW` → `PENDING_PAYMENT` → `PAID` → `TICKETS_ISSUED`. Turn 1 chỉ trả về thẻ danh sách chuyến. | **PASS** |
| **4** | Đề xuất ghế A1, A10 cạnh nhau | `getAvailableSeats` dùng Java String comparator khiến `"A10"` đứng ngay sau `"A1"`. Hàm `suggestAdjacentSeats` chỉ kiểm tra `num2 - num1 != 1` mà không phân tích tiền tố dãy ghế. | Thiết kế `compareSeatNames` sắp xếp Alphanumeric tự nhiên (`A1, A2, ..., A9, A10, B1...`). Tách tiền tố và số thứ tự; thuật toán ghép cặp cùng tầng, cùng dãy liền kề hoặc đối xứng hàng ngang. Không bao giờ ghép A1 với A10. | **PASS** |
| **5** | AI quên điểm khởi hành Sài Gòn | Khi người dùng trả lời điểm đi ở lượt 2, `isDirectBookingIntent` trả về `false` do câu quá ngắn, làm router đẩy nhầm sang FAQ chung. | Bổ sung `hasActiveBookingFlow(sessionKey)` trong `ChatbotService` để ưu tiên giữ luồng đặt vé; kế thừa và chuẩn hóa địa điểm qua `VietnameseLocationNormalizer` ("Sài Gòn" ↔ "TP. Hồ Chí Minh"). | **PASS** |
| **6** | Chatbot bị gián đoạn kết nối AI | Mất ngữ cảnh phiên giữa các HTTP request stateless, thiếu cơ chế phục hồi khi ChatClient Spring AI gặp timeout/lỗi. | Quản lý `BookingSessionManager` lưu trữ TTL 30 phút trên Redis / ConcurrentHashMap fallback; đồng bộ `sessionId` và `conversationId` từ Frontend `localStorage`. | **PASS** |
| **7** | Interactive Cards và AI message lệch trạng thái | Các nút bấm trên các card cũ trong lịch sử chat vẫn cho phép click lại, ghi đè state mới của phiên hiện tại. | Frontend `ChatbotWidget.tsx` gắn cờ `isLatestMessage`. Các nút bấm ở tin nhắn cũ bị khóa/disable hoặc chuyển sang chế độ hiển thị tham khảo, chống click đúp với `isSubmitting`. | **PASS** |
| **8** | Báo "Đặt vé thành công" khi đang `PENDING_PAYMENT` | Header phản hồi của AI hardcode `"🎉 ĐẶT VÉ THÀNH CÔNG!"` ngay khi booking mới được tạo ở trạng thái giữ chỗ `PENDING_PAYMENT`. | Tách bạch rõ 2 trạng thái: <br>- `PENDING_PAYMENT`: **"📋 ĐÃ GIỮ CHỖ THÀNH CÔNG — CHỜ THANH TOÁN"** (kèm hạn 5 phút).<br>- `PAID`: **"🎉 THANH TOÁN MÔ PHỎNG THÀNH CÔNG!"** (sau khi quét QR/gọi Demo Payment). | **PASS** |

---

## 3. THIẾT KẾ STATE MACHINE & WORKFLOW BACKEND

```
 [COLLECTING_INFO]
        │  (Người dùng cung cấp điểm đi, điểm đến, ngày giờ)
        ▼
 [TRIP_SELECTION]  ──► Trả về danh sách chuyến xe (Card Loại 1)
        │  (Người dùng click "Chọn chuyến này" hoặc nhắn "chuyến 20:45")
        ▼
 [SEAT_SELECTION]  ──► Khóa selectedTripId, hỏi chọn ghế liền kề (Card Loại 2)
        │  (Người dùng chọn "Có, chọn 2 ghế cạnh nhau" hoặc chỉ định mã ghế)
        ▼
 [BOOKING_REVIEW]  ──► Tạm giữ chỗ qua Redis, tạo Draft và hiển thị Xác nhận (Card Loại 3)
        │  (Người dùng click "Xác nhận và thanh toán")
        ▼
 [PENDING_PAYMENT] ──► Tạo Booking DB trạng thái PENDING_PAYMENT, hiển thị Demo QR (Card Loại 4)
        │              Header: "📋 ĐÃ GIỮ CHỖ THÀNH CÔNG — CHỜ THANH TOÁN"
        │  (Người dùng quét QR / click "Xác nhận thanh toán mô phỏng")
        ▼
 [PAID]            ──► Gọi DemoPaymentProcessor.processPayment, phát hành vé điện tử (Card Loại 5)
        │              Header: "🎉 THANH TOÁN MÔ PHỎNG THÀNH CÔNG!"
        ▼
 [TICKETS_ISSUED]
```

---

## 4. KẾT QUẢ KIỂM THỬ THỰC TẾ (TEST SUITES)

1. **`BookingAssistantTest` (Backend Unit & State Regression Suite)**:
   - Số test: **23/23 tests PASS** (0 failures, 0 errors, 0 skipped).
   - Kiểm thử đầy đủ: `testIssue1_PassengerCountPreservedAcrossMultiTurns`, `testIssue2_TripTimeMatchingPreservesSelectedTrip`, `testIssue3_StateStepTripSelectionDisplayedBeforeConfirmation`, `testIssue4_SeatAdjacencyNeverPairsA1AndA10`, `testIssue5_LocationNormalizedAndRetainedAcrossTurns`, `testIssue6_SessionStateFlowAcrossBookingSteps`, `testIssue7_SelectedTripIdLockedAgainstHijacking`, `testIssue8_PendingPaymentDisplaysGiuchothanhcongPaidDisplaysDathanhtoan`.

2. **`TicketCancellationTest` (AI Cancellation Tool Suite)**:
   - Số test: **18/18 tests PASS** (0 failures, 0 errors, 0 skipped).

3. **`DemoPaymentTest` (Simulated Payment & Security Guard Suite)**:
   - Số test: **18/18 tests PASS** (0 failures, 0 errors, 0 skipped).

4. **`UnifiedDatabaseDataVerificationTest` (Shared PostgreSQL & Real Data Verification)**:
   - Số test: **4/4 tests PASS** (0 failures, 0 errors, 0 skipped).
   - Dữ liệu thực tế được xác minh trong DB `busbooking`: `routes=83`, `trips=50647`, `bookings=13299`, `tickets=26340`, `payments=10922`.

5. **`Nhom01_WWW_Frontend` (TypeScript & Vite Build)**:
   - Lệnh: `npm --prefix D:\WWW\project\Nhom01_WWW_Frontend run build`.
   - Kết quả: **Build thành công** với 2,138 modules transformed, 0 lỗi TypeScript.
