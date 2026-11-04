# Hướng dẫn kiểm thử (Testing Guide)

Tài liệu này hướng dẫn chi tiết cách chạy và xác minh bộ kiểm thử tự động gồm 18 kịch bản nghiệp vụ cho tính năng AI Ticket Cancellation Assistant.

---

## 1. Lệnh thực thi kiểm thử

### 1.1. Chạy riêng bộ kiểm thử Hủy vé (`TicketCancellationTest`)
Mở PowerShell tại thư mục gốc backend:
```powershell
.\mvnw.cmd test "-Dtest=TicketCancellationTest"
```

### 1.2. Chạy kết hợp với bộ kiểm thử Thanh toán Demo (`DemoPaymentTest`)
```powershell
.\mvnw.cmd test "-Dtest=TicketCancellationTest,DemoPaymentTest"
```

### 1.3. Chạy toàn bộ test suite của dự án
```powershell
.\mvnw.cmd test
```

---

## 2. Chi tiết 18 kịch bản kiểm thử (Test Scenarios)

| # | Tên kiểm thử | Mục đích & Nghiệp vụ kiểm tra | Kết quả |
|:---|:---|:---|:---:|
| 1 | `testCancelAllTicketsInBooking` | Hủy toàn bộ vé trong đơn -> Booking chuyển thành `CANCELLED`, ghế giải phóng. | **PASS** |
| 2 | `testCancelSingleTicketInMultiTicketBooking` | Hủy 1 trong 2 vé -> Booking chuyển `PARTIALLY_CANCELLED`, vé còn lại nguyên vẹn. | **PASS** |
| 3 | `testBookingBelongsToAnotherCustomer` | Khách A cố tình hủy đơn của khách B -> Bị từ chối bằng exception / lỗi bảo mật. | **PASS** |
| 4 | `testBookingAlreadyCancelled` | Đơn đã ở trạng thái `CANCELLED` -> Bị từ chối, không được hủy lại. | **PASS** |
| 5 | `testUsedTicketCannotBeCancelled` | Vé có trạng thái `USED` (đã đi xe) -> Bị từ chối hủy vé. | **PASS** |
| 6 | `testPastCancellationDeadline` | Thời gian đến giờ khởi hành < 2 giờ -> Không đủ điều kiện hủy vé (`isEligible = false`). | **PASS** |
| 7 | `testCancelUnpaidPendingBooking` | Đơn chưa thanh toán (`PENDING_PAYMENT`) -> Hủy giữ chỗ ngay lập tức, hoàn tiền = 0. | **PASS** |
| 8 | `testDemoRefundSuccess` | Đơn đã thanh toán bằng DEMO_PAYMENT -> Hoàn tiền ví thành công, trạng thái `REFUNDED`. | **PASS** |
| 9 | `testDoubleClickConfirmationIdempotency` | Bấm nút xác nhận 2 lần với cùng 1 token -> Kết quả trả về giữ nguyên, không hủy lặp lại. | **PASS** |
| 10 | `testPreventDuplicateRefund` | Cố tình gọi hoàn tiền lần thứ 2 trên cùng 1 đơn -> Ngăn chặn hoàn tiền kép. | **PASS** |
| 11 | `testConcurrentCancellationControl` | 2 luồng đồng thời gọi xác nhận hủy cùng đơn -> Chỉ 1 luồng thành công, 1 luồng trả kết quả an toàn. | **PASS** |
| 12 | `testUserChangesMindKeepTickets` | Người dùng chọn "Giữ lại vé" -> Trạng thái đơn và ghế không đổi. | **PASS** |
| 13 | `testAiWrongBookingId` | AI truyền sai mã booking không tồn tại -> Báo lỗi `NOT_FOUND` rõ ràng, không crash. | **PASS** |
| 14 | `testPromptInjectionBlocked` | Tin nhắn độc hại chứa prompt injection -> Bị parser chặn và trả về từ chối an toàn. | **PASS** |
| 15 | `testPreviewTokenExpired` | Xác nhận hủy sau khi token hết hạn (> 5 phút) -> Trả về lỗi `410 GONE` / Token Expired. | **PASS** |
| 16 | `testRefundTransactionSafety` | Giao dịch hoàn tiền được bảo bọc trong `@Transactional`, rollback khi có lỗi xảy ra. | **PASS** |
| 17 | `testSeatReleasedProperlyUponCancellation` | Sau khi hủy, ghế biến mất khỏi danh sách `findBookedSeatIdsByTripId()`. | **PASS** |
| 18 | `testRemainingTicketsUnaffectedByPartialCancellation` | Hủy 1 vé trong đơn nhiều vé -> Vé còn lại vẫn giữ đúng mã số ghế và trạng thái `ISSUED`. | **PASS** |

---

## 3. Kiểm thử trên giao diện người dùng (Manual UI Test)
1. Khởi động backend: `.\mvnw.cmd spring-boot:run`
2. Khởi động frontend: `npm --prefix D:\WWW\project\Nhom01_WWW_Frontend run dev`
3. Đăng nhập tài khoản khách hàng demo (ví dụ: `customer1@gmail.com`).
4. Đặt 1 đơn vé xe hoặc sử dụng đơn đã có trong lịch sử đặt vé.
5. Mở chatbot và nhắn: `"Hủy giúp tôi 2 vé đi Đà Lạt tối mai"`.
6. Xác nhận giao diện hiển thị đúng `CancellationPreviewCard` với tiền phí và tiền hoàn tính toán chính xác theo giờ khởi hành.
7. Nhấn "Xác nhận hủy vé" và kiểm tra phản hồi `CancellationSuccessCard` cùng trạng thái ghế được giải phóng trên trang Đặt vé.
