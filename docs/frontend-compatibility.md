# Đồng bộ ứng dụng web VEXE

## Các contract được giữ và bổ sung

- Giữ `/api/auth/login` dành cho nhân viên/mobile. Web dùng `POST /api/auth/web-login` với `{ "email": "...", "password": "..." }` (hoặc `username`). Web nhận access token/refresh token tại `data`; khách hàng ACTIVE được phép đăng nhập, tài khoản khóa vẫn bị từ chối.
- Giữ API quản lý tài khoản phân trang: danh sách nằm ở `data.content`, không phải `data`. Frontend gửi keyword/status/page/size và có nút chuyển trang. DTO nhân viên có `roleId`/`roleName`, service frontend chuyển thành `role` để hiển thị.
- Quản lý nhân viên và reset mật khẩu vẫn chỉ cho Admin. Form tạo nhân viên yêu cầu mật khẩu ban đầu; reset yêu cầu `{ "newPassword": "..." }`. Không dùng mật khẩu mặc định công khai.
- Bổ sung `GET /api/web/bookings/my-history` (chỉ lịch sử khách hàng hiện tại) và `GET /api/web/bookings` (Admin hoặc quyền quản lý vé). DTO trả `bookingCode`, `seats`, thông tin chuyến/khách hàng tối thiểu và thông tin yêu cầu hủy; không trả password/entity graph.
- `cancelRequestId` là ID yêu cầu hủy, khác ID booking. Frontend gửi yêu cầu bằng `POST /api/cancel-requests` với `bookingId`/`reason`, duyệt hoặc từ chối bằng `PATCH /api/cancel-requests/{cancelRequestId}/resolve`. Từ chối bắt buộc lý do. Không báo thành công nếu API thất bại; không chèn lịch sử/vé mẫu khi danh sách rỗng.
- Duyệt hủy không hoàn tiền VNPay. Một booking chỉ có một yêu cầu hủy theo chính sách hiện tại; `canCancel` phản ánh điều kiện được gửi yêu cầu.
- Thêm/sửa chuyến xe đã lưu dữ liệu thực; tìm chuyến lọc điểm đi/đến (không phân biệt hoa thường) và ngày, sắp xếp theo giờ khởi hành. Tham số trống được bỏ qua. Không cho đổi xe/tuyến của chuyến đã có booking. Frontend gửi LocalDateTime không đổi sang UTC.
- Frontend `/admin/reports` hiển thị API `/api/admin/statistics/revenue`: lọc khoảng ngày, nhóm DAY/MONTH, tổng tiền đã thu, số giao dịch, tiền thuộc đơn đã hủy, biểu đồ theo kỳ và bảng phương thức thanh toán. Chỉ Admin hoặc tài khoản có VIEW_STATISTICS được xem. Có trạng thái tải/lỗi/rỗng; không dùng dữ liệu giả và không coi tiền thuộc đơn hủy là tiền đã hoàn.

## Chạy lại

Giữ PostgreSQL/Redis đang chạy. Khởi động lại backend **không kèm `--seed`**, sau đó frontend. Đăng xuất và đăng nhập lại để lấy token/quyền mới.

```powershell
# Terminal backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8080"
```

```powershell
# Terminal frontend
npm run dev
```

Frontend `.env`: `VITE_API_URL=http://localhost:8080/api`.

## Kiểm tra

```powershell
# Backend, dùng JDK 17; loại context test cần hạ tầng thật
.\mvnw.cmd test '-Dtest=!*Nhom01WwwApplicationTests'
```

```powershell
# Frontend
npm run build
npm run lint
```

Cần kiểm thử tích hợp thêm trên PostgreSQL/Redis và trình duyệt. Không chạy seeder trên dữ liệu cần giữ.

### Kiểm thử truy vấn doanh thu trên PostgreSQL thật (chỉ đọc)

`PaymentRevenuePostgresTest` kiểm tra chính SQL trong PaymentRepository cho cả DAY/MONTH, tên cột projection và tổng tiền/số giao dịch so với truy vấn đối chiếu. Không khởi động ứng dụng, không INSERT/UPDATE/DELETE và không chạy seeder. Test mặc định bỏ qua khi chưa đặt biến môi trường kết nối:

```powershell
$env:VEXE_REPORT_TEST_URL = 'jdbc:postgresql://localhost:6666/busbooking'
$env:VEXE_REPORT_TEST_USER = 'postgres'
$env:VEXE_REPORT_TEST_PASSWORD = '<database-password>'
.\mvnw.cmd test '-Dtest=PaymentRevenuePostgresTest'
```

Truy vấn nhóm bằng vị trí cột (`GROUP BY 1, 2`) để tránh lỗi PostgreSQL khi biểu thức định dạng ngày trong SELECT/GROUP BY dùng hai JDBC placeholder khác nhau.

## Ngoài phạm vi đợt này

Luồng chọn ghế/giữ ghế/thanh toán tại frontend vẫn là mô phỏng, QR và tải vé chưa được nối với VNPay thật. Các rủi ro cũ của webhook thanh toán và API tra cứu công khai chưa được xử lý trong đợt đồng bộ này. Không triển khai production chỉ dựa trên các test tương thích.
