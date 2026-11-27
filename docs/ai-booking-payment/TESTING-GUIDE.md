# HƯỚNG DẪN KIỂM THỬ (TESTING GUIDE)

Tài liệu này hướng dẫn cách chạy toàn bộ test suite và mô tả chi tiết 18 kịch bản kiểm thử (Test Scenarios) cho tính năng AI Booking & QR Demo Payment.

---

## 1. CÁCH CHẠY TEST TỰ ĐỘNG

### 1.1 Chạy riêng bộ test 18 kịch bản Demo Payment
```powershell
.\mvnw.cmd test "-Dtest=DemoPaymentTest"
```
Kết quả mong đợi: `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0` - `BUILD SUCCESS`.

### 1.2 Chạy bộ test AI Booking Assistant
```powershell
.\mvnw.cmd test "-Dtest=BookingAssistantTest"
```
Kết quả mong đợi: `Tests run: 15, Failures: 0, Errors: 0, Skipped: 0` - `BUILD SUCCESS`.

### 1.3 Chạy toàn bộ test suite dự án
```powershell
.\mvnw.cmd test
```
Kết quả mong đợi: `Tests run: 127, Failures: 0, Errors: 0, Skipped: 6` - `BUILD SUCCESS`.

---

## 2. CHI TIẾT 18 KỊCH BẢN KIỂM THỬ BẮT BUỘC

| STT | Tên Kịch Bản | Phương Thức / Class | Kết Quả Mong Đợi | Trạng Thái |
|---|---|---|---|---|
| **1** | Tìm chuyến xe thành công | `test1_SearchTripSuccess` | Trả về danh sách chuyến hợp lệ, đúng tuyến, giờ đi, xe, giá và số chỗ trống thực tế | **PASSED** |
| **2** | Không tìm thấy chuyến phù hợp | `test2_NoMatchingTrip` | Trả về danh sách rỗng, không bịa đặt dữ liệu ảo | **PASSED** |
| **3** | Thiếu điểm đi — AI hỏi lại điểm đi | `test3_MissingDeparturePrompt` | Status: `NEED_INFO`, câu trả lời lịch sự hỏi điểm xuất phát | **PASSED** |
| **4** | Nhận diện Sài Gòn = TP.HCM | `test4_SaiGonEqualsTpHcmRecognition` | Normalizer chuyển cả "Sài Gòn" và "TP. Hồ Chí Minh" về `tp hcm`, tìm thấy chuyến | **PASSED** |
| **5** | Đề xuất 2 ghế cạnh nhau thành công | `test5_SuggestAdjacentSeatsSuccess` | Dựa theo sơ đồ ghế thực tế, trả về đúng cặp ghế liền kề: `[A01, A02]` trên Tầng 1 | **PASSED** |
| **6** | Không đủ 2 ghế cạnh nhau | `test6_NotEnoughAdjacentSeats` | Khi chỉ còn các ghế lẻ cách quãng (A01, A04), hệ thống trả về danh sách rỗng | **PASSED** |
| **7** | Xác nhận đặt vé thành công | `test7_ConfirmBookingSuccess` | Tạo booking trạng thái `PENDING_PAYMENT`, trả về `bookingId`, `bookingCode` | **PASSED** |
| **8** | Hủy đặt vé | `test8_CancelBooking` | Trạng thái `REJECTED`, xóa draft đang chờ, không tạo booking | **PASSED** |
| **9** | Sinh mã QR demo thành công | `test9_GenerateDemoQrSuccess` | Trả về chuỗi Base64 hình QR, trạng thái `PENDING`, có ghi chú `THANH TOÁN MÔ PHỎNG` | **PASSED** |
| **10** | Mô phỏng thanh toán thành công | `test10_SimulatedPaymentSuccess` | `Payment` -> `SIMULATED_PAID`, `Booking` -> `PAID`, phát hành vé điện tử `ISSUED` | **PASSED** |
| **11** | Thanh toán trùng lặp (Idempotent) | `test11_DuplicatePaymentIdempotent` | Nếu booking đã `PAID`, trả về ngay kết quả trước đó mà không trừ tiền hoặc tạo lại vé | **PASSED** |
| **12** | Đặt vé hết hạn giữ chỗ bị từ chối | `test12_ExpiredBookingRejection` | Khi Redis báo hết 5 phút giữ chỗ, ném lỗi `410 GONE` | **PASSED** |
| **13** | Xung đột đặt trùng ghế | `test13_ConcurrentSeatConflict` | Ghế đã có khách khác giữ trong Redis không xuất hiện trong danh sách ghế trống | **PASSED** |
| **14** | Không có quyền thanh toán booking của người khác | `test14_UnauthorizedUserPaymentRejection` | Kiểm tra quyền sở hữu với `customerId`, nếu khác ném lỗi `403 FORBIDDEN` | **PASSED** |
| **15** | AI không tự ý confirm khi chưa có hành động | `test15_AiCannotAutoConfirmWithoutUserAction` | Kiểm tra chặt chẽ `bookingService.createBooking` KHÔNG BAO GIỜ bị gọi lén | **PASSED** |
| **16** | Phân biệt mã QR vé điện tử và mã QR demo | `test16_TicketQrDistinctFromPaymentQr` | QR Demo có `DEMO_PAYMENT`, QR vé có `TICKET:`. Hai mã hoàn toàn khác nhau | **PASSED** |
| **17** | Phục hồi trạng thái đơn đặt vé khi reload | `test17_FrontendRefreshPreservesBookingState` | Tra cứu qua `bookingTools.getBookingStatus(501L)` lấy đúng trạng thái PENDING | **PASSED** |
| **18** | Production chặn endpoint demo | `test18_ProductionRejectionWhenDisabled` | Khi `app.demo-payment.enabled=false`, lập tức ném `403 FORBIDDEN` | **PASSED** |
