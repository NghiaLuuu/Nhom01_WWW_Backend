# Xử Lý Sự Cố & Vận Hành (Troubleshooting Guide)

## 1. Sự cố OpenAI API bị lỗi hoặc gián đoạn mạng
* **Hiện tượng**: Log xuất hiện thông báo `ChatClient call exception` (ví dụ `401 Unauthorized`, `429 Too Many Requests`, hoặc `SocketTimeoutException`).
* **Cơ chế xử lý**: 
  - Hệ thống tự động kích hoạt **Deterministic Workflow Fallback** (`executeDeterministicBookingWorkflow`).
  - Toàn bộ việc bóc tách thực thể tiếng Việt được xử lý mượt mà bởi `BookingIntentParser` nội bộ.
  - Các Booking Tools (`searchAvailableTrips`, `prepareBooking`, `confirmBooking`) vẫn được gọi trực tiếp bằng logic Java an toàn.
  - Người dùng vẫn nhận được câu trả lời đầy đủ, chính xác và đặt vé bình thường mà không bị gián đoạn dịch vụ.

## 2. Lỗi "Ghế đang trong thời gian giữ chỗ của người khác" (409 Conflict)
* **Nguyên nhân**: Trong khoảng thời gian 5 phút kể từ khi một khách hàng khác ấn đặt vé, các ghế đó được giữ chỗ độc quyền trong Redis (`seat_hold:trip:{tripId}:seat:{seatId}`).
* **Xử lý**:
  - AI Assistant thông báo rõ ràng cho khách hàng rằng ghế vừa có người khác giữ chỗ.
  - Hệ thống tự động gợi ý danh sách các ghế trống còn lại hoặc các chuyến xe cùng ngày.
  - Nếu sau 5 phút khách hàng trước không thanh toán, `@Scheduled` job `releaseExpiredBookings` trong `BookingServiceImpl` sẽ tự động giải phóng key và đưa ghế về trạng thái sẵn sàng.

## 3. Lỗi "Quý khách cần đăng nhập tài khoản trước khi xác nhận đặt vé" (401 Unauthorized)
* **Nguyên nhân**: Người dùng gửi yêu cầu xác nhận đơn hàng khi chưa đăng nhập hoặc token JWT đã hết hạn.
* **Xử lý**:
  - Không được lưu đơn vé vô chủ.
  - AI Assistant nhắc nhở người dùng đăng nhập tài khoản. Bản nháp đặt vé vẫn được giữ tạm thời trong `BookingDraftManager` (TTL 15 phút) để sau khi đăng nhập xong, người dùng có thể nói "Xác nhận" để hoàn tất đơn hàng ngay mà không cần chọn lại từ đầu.

## 4. Xử lý lỗi Idempotency khi người dùng bấm xác nhận nhiều lần
* **Hiện tượng**: Do mạng chập chờn, client gửi 2 request "Tôi đồng ý" cùng lúc hoặc liên tiếp.
* **Xử lý**:
  - Lượt đầu tiên: tạo đơn trong database, lưu kết quả vào `BookingDraftManager.registerIdempotentResult`.
  - Lượt thứ hai: nhận diện `idempotencyKey` đã có, trả về ngay kết quả của lượt đầu với `isAlreadyExisting = true`, không trừ tiền hay tạo thêm bất kỳ booking trùng lặp nào.
