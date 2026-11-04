# Hướng dẫn xử lý sự cố (Troubleshooting Guide)

Tài liệu này tổng hợp các tình huống lỗi thường gặp khi triển khai hoặc vận hành tính năng AI Ticket Cancellation Assistant và giải pháp khắc phục.

---

## 1. Lỗi: "Yêu cầu xem trước hủy vé đã hết hạn" (Token Expired / 410 GONE)
- **Triệu chứng:** Người dùng xem thẻ `CancellationPreviewCard` quá 5 phút mới bấm nút "Xác nhận hủy vé", hệ thống báo lỗi không thể thực hiện.
- **Nguyên nhân:** Để chống việc biến động giá vé hoặc sai lệch thời gian khởi hành (ảnh hưởng tới bậc phần trăm hoàn tiền), mỗi `confirmationToken` chỉ có vòng đời sống 300 giây (5 phút) trong `CancellationPreviewManager`.
- **Cách xử lý:** 
  - Đây là cơ chế bảo vệ an toàn chủ đích của hệ thống.
  - Người dùng chỉ cần gửi lại yêu cầu hủy vé hoặc nhấn chọn lại đơn vé để hệ thống sinh token mới với thông số tính toán cập nhật nhất.

---

## 2. Lỗi: "Bạn không có quyền truy cập đơn đặt vé này" (403 FORBIDDEN)
- **Triệu chứng:** Người dùng yêu cầu hủy một đơn đặt vé cụ thể theo mã nhưng nhận được thông báo không tìm thấy hoặc bị từ chối truy cập.
- **Nguyên nhân:** 
  - Hệ thống áp dụng nguyên tắc bảo mật Zero-Trust: Mã khách hàng (`customerId`) được lấy tuyệt đối từ phiên đăng nhập hiện tại (`SecurityContextHolder`).
  - Nếu khách hàng đang đăng nhập bằng tài khoản A nhưng cố tình hủy đơn vé thuộc về khách hàng B, request sẽ lập tức bị chặn.
- **Cách xử lý:**
  - Kiểm tra xem người dùng đã đăng nhập đúng tài khoản đã đặt vé đó chưa.
  - Sử dụng chức năng "Xem đơn vé của tôi" trên thanh điều hướng để kiểm tra mã đơn chính xác của tài khoản hiện hành.

---

## 3. Lỗi: Ghế không hiển thị lại trên sơ đồ đặt vé sau khi hủy
- **Triệu chứng:** Đơn vé đã báo hủy thành công trên Chatbot, nhưng khách hàng khác vào đặt chuyến xe đó vẫn thấy ghế đang ở trạng thái màu đỏ (đã đặt).
- **Nguyên nhân kiểm tra:**
  - Kiểm tra phương thức truy vấn danh sách ghế đã đặt: `TicketRepository.findBookedSeatIdsByTripId(tripId)`.
  - Nếu query còn sót điều kiện hoặc cache chuyến xe (`@Cacheable`) chưa được evict sau khi hủy vé, client sẽ đọc dữ liệu cũ từ cache.
- **Giải pháp:**
  - Đảm bảo vé được đổi `status = 'CANCELLED'`.
  - Khuyến nghị evict cache trip availability: `@CacheEvict(value = "trip-availability", key = "#tripId")` khi thực hiện `cancelTicket()`.

---

## 4. Lỗi: Double Click dẫn đến hoàn tiền hai lần
- **Triệu chứng:** Mạng lag hoặc người dùng nhấn nút "Xác nhận hủy vé" liên tiếp nhiều lần.
- **Biện pháp phòng ngừa đã triển khai:**
  - Tại Frontend: Nút "Xác nhận hủy vé" ngay lập tức bị disable (`isSubmitting = true`) sau lần click đầu tiên.
  - Tại Backend: `CancellationPreviewManager` xóa token ngay trong lần đầu đọc (`invalidate`), và ghi cache kết quả vào `idempotencyResultMap`. Nếu request thứ hai gửi cùng token đến, nó sẽ nhận lại ngay kết quả của lần xử lý đầu tiên mà không chạy lại logic hoàn tiền.
