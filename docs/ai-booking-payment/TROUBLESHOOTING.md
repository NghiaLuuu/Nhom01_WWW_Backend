# HƯỚNG DẪN XỬ LÝ SỰ CỐ (TROUBLESHOOTING)

Tài liệu này cung cấp các giải pháp khắc phục sự cố kỹ thuật thường gặp trong quá trình vận hành AI Booking và Thanh toán Demo.

---

## 1. LỖI SINH MÃ QR (QR GENERATION ERROR)

### Triệu chứng:
Chatbot hiển thị thông báo: *"Không thể tạo mã QR thanh toán"* hoặc hình ảnh QR bị vỡ/trắng.

### Nguyên nhân:
- Thiếu thư viện tạo mã QR (`com.google.zxing:core` và `com.google.zxing:javase`) trong classpath.
- Kích thước QR truyền vào bị âm hoặc vượt giới hạn bộ nhớ heap.

### Cách xử lý:
1. Kiểm tra lớp `QRCodeGenerator.java` nằm tại `vn.edu.iuh.nhom01_www.common.util.QRCodeGenerator`.
2. Đảm bảo cấu hình kích thước chuẩn `300 x 300` px.
3. Kiểm tra tiền tố chuỗi Base64: Chuỗi phải có định dạng `data:image/png;base64,...` để thẻ `<img src="...">` trên frontend render trực tiếp mà không cần lưu file tạm trên đĩa.

---

## 2. LỖI HẾT HẠN GIỮ CHỖ 5 PHÚT (`410 GONE`)

### Triệu chứng:
Người dùng nhấn nút "Mô phỏng thanh toán thành công" nhưng nhận được thông báo lỗi:
> *"Đơn đặt vé đã hết hạn giữ chỗ và bị hủy tự động. Vui lòng đặt lại vé mới!"*

### Nguyên nhân:
- Quá 300 giây (5 phút) kể từ khi tạo booking, key giữ chỗ trên Redis (`seat_hold:booking:{id}`) đã hết thời hạn TTL và tự động bị Redis xóa.
- Hệ thống bảo vệ tự động hủy đơn giữ chỗ quá hạn để trả ghế lại cho khách khác đặt.

### Cách xử lý:
1. Đây là hành vi nghiệp vụ **hoàn toàn chính xác** của hệ thống bán vé xe nhằm chống găm giữ ghế ảo.
2. Để tăng thời gian thử nghiệm khi thuyết trình demo, có thể điều chỉnh cấu hình TTL trong `SeatHoldRedisServiceImpl`:
   ```java
   private static final long HOLD_MINUTES = 10; // Tăng lên 10 phút nếu cần demo dài
   ```
3. Đảm bảo dịch vụ Redis đang chạy bình thường (`redis-cli ping` trả về `PONG`).

---

## 3. LỖI XUNG ĐỘT GHẾ KHI 2 NGƯỜI CÙNG ĐẶT (`409 CONFLICT`)

### Triệu chứng:
Cả 2 khách hàng cùng thấy ghế A01 trống, nhưng người thứ hai nhấn xác nhận thì bị báo lỗi ghế đã bị giữ.

### Nguyên nhân:
- Người thứ nhất đã hoàn thành bước xác nhận trước, Redis ghi nhận `seatHoldRedisService.holdSeats(...)` thành công.
- Người thứ hai gửi request sau sẽ bị Redis chặn để đảm bảo tính nhất quán (Atomicity).

### Cách xử lý:
- AI chatbot tự động thông báo: *"Dạ các ghế trên chuyến xe này vừa được khách hàng khác giữ chỗ. Quý khách vui lòng chọn ghế khác giúp em nhé!"*.
- Người dùng chỉ cần chọn ghế khác (ví dụ A03 hoặc A04) và tiếp tục luồng đặt vé.

---

## 4. LỖI 403 FORBIDDEN KHI THANH TOÁN DEMO TRÊN PRODUCTION

### Triệu chứng:
Endpoint `/api/payments/demo/confirm` hoặc `/create-qr` trả về lỗi HTTP 403.

### Nguyên nhân:
- Môi trường production kích hoạt cơ chế bảo vệ: `app.demo-payment.enabled = false`.
- Hệ thống tuyệt đối không cho phép mô phỏng thanh toán hoặc phát hành vé không trả tiền thật trên môi trường thật.

### Cách xử lý:
- Trong môi trường local / staging / demo đồ án, kiểm tra file `application.yml`:
  ```yaml
  app:
    demo-payment:
      enabled: true
  ```
- Khởi động lại backend để cờ cấu hình có hiệu lực.
