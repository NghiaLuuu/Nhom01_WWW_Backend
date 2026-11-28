# API quản trị khách hàng, nhân viên, hủy vé và doanh thu

Tất cả API yêu cầu `Authorization: Bearer <access-token>`. Response giữ envelope:

```json
{"success": true, "message": "Success", "data": {}}
```

## 1. Quyền truy cập

| Nhóm API | Quyền |
|---|---|
| Quản lý nhân viên, đặt lại mật khẩu | `ROLE_ADMIN` hoặc `ADMIN` |
| Tra cứu/khóa khách hàng | Admin hoặc `CUSTOMER_MANAGE` |
| Xem, duyệt/từ chối yêu cầu hủy | Admin hoặc `TICKET_MANAGE` / `MANAGE_TICKET` |
| Báo cáo doanh thu | Admin hoặc `VIEW_STATISTICS` |
| Gửi yêu cầu hủy | Khách hàng đăng nhập, sở hữu booking |

`STAFF_MANAGE` riêng lẻ không được cấp quyền quản lý tài khoản nhân viên nữa: chỉ Admin được thay đổi role/quyền để tránh nâng quyền ngoài ý muốn. Frontend nên ẩn màn hình tương ứng khi không phải Admin.

## 2. Danh sách, phân trang và tìm kiếm

```http
GET /api/admin/customers?keyword=090&status=ACTIVE&page=0&size=20
GET /api/admin/staffs?keyword=NV001&page=0&size=20
```

- `keyword`: tối đa 200 ký tự; tìm chứa tên/email/điện thoại, thêm mã nhân viên cho staffs. Tên/email/mã không phân biệt hoa thường; `%` và `_` được hiểu là ký tự thường.
- `status`: tùy chọn `ACTIVE`, `LOCKED`, `PENDING_VERIFICATION`.
- `page`: từ 0; `size`: 1–100; mặc định 20. Sắp xếp mới nhất, dùng id để ổn định thứ tự.
- **Thay đổi contract:** `data` từng là mảng, nay là object phân trang. Frontend lấy danh sách tại `data.content`.

```json
{"content": [], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0}
```

DTO không trả password, token hay thông tin bảo mật. Khách hàng có id, tên, email, điện thoại, ngày sinh, địa chỉ, trạng thái và ngày tạo. Nhân viên thêm mã, vị trí, nơi làm việc, ngày vào làm, role và quyền riêng.

## 3. Chi tiết và lịch sử khách hàng

```http
GET /api/admin/customers/{uuid}
GET /api/admin/customers/{uuid}/bookings?page=0&size=20
GET /api/admin/staffs/{uuid}
```

Lịch sử booking trả bản tóm tắt: `id`, `tripId`, `status`, `totalPrice`, `createdAt`, không trả entity graph hay khách hàng khác. Không tìm thấy tài khoản trả 404.

## 4. Tạo/cập nhật nhân viên

```http
POST /api/admin/staffs
Content-Type: application/json
```

```json
{
  "email": "staff.new@example.com",
  "fullName": "Nguyễn Văn An",
  "password": "Replace-with-a-unique-password",
  "staffCode": "NV001",
  "phoneNumber": "0901234000",
  "dateOfBirth": "1998-05-15",
  "position": "Nhân viên bán vé",
  "workStation": "Bến xe Miền Đông",
  "roleId": 2,
  "permissions": []
}
```

- Email, fullName và password bắt buộc khi tạo. Password tối thiểu 8 ký tự, tối đa 72 byte UTF-8 (BCrypt).
- Không truyền `staffCode`: sinh mã `NV` + UUID, không dùng timestamp rút gọn.
- Không truyền `roleId`: dùng role `ROLE_STAFF`. Role phải tồn tại và thuộc `ROLE_STAFF`/`ROLE_ADMIN`.
- Mã nhân viên 2–40 ký tự chữ/số/`_`/`-`; điện thoại 8–15 chữ số, có thể có `+` đầu; ngày sinh phải trong quá khứ.
- Email không được trùng tài khoản nhân viên/khách hàng; mã nhân viên và điện thoại không trùng nhân viên khác. Trùng trả 409.
- `permissions` chỉ nhận code quyền tồn tại trong DB.

```http
PUT /api/admin/staffs/{uuid}
```

Cùng cấu trúc body; chỉ cập nhật trường khác null. Không xóa trường bằng null. Không cho Admin đổi role của chính mình.

## 5. Khóa/mở khóa và đặt lại mật khẩu

```http
PATCH /api/admin/customers/{uuid}/status
PATCH /api/admin/staffs/{uuid}/status
```

```json
{"status": "LOCKED"}
```

Chỉ nhận `ACTIVE` hoặc `LOCKED`. Không cho khóa tài khoản nhân viên đang đăng nhập. Tài khoản khóa không đăng nhập và không tiếp tục dùng JWT đã cấp. Cập nhật entity cũng làm thay đổi version, vô hiệu hóa JWT cũ theo cơ chế sẵn có.

```http
PATCH /api/admin/customers/{uuid}/reset-password
PATCH /api/admin/staffs/{uuid}/reset-password
```

```json
{"newPassword": "Replace-with-a-unique-password"}
```

**Thay đổi contract:** bắt buộc body mật khẩu mới; bỏ mật khẩu mặc định công khai `123456`/`Vexe@123`. Không ảnh hưởng API quên mật khẩu bằng OTP hiện có. Sau khi reset, JWT cũ không còn hợp lệ; cung cấp mật khẩu mới qua kênh an toàn ngoài API response.

## 6. Yêu cầu hủy toàn bộ booking

```http
POST /api/cancel-requests
```

```json
{"bookingId": 123, "reason": "Thay đổi lịch trình"}
```

Quy tắc đang áp dụng (cần thống nhất thêm nếu có chính sách khác):
- Chỉ chủ booking được gửi yêu cầu; booking phải `PAID`.
- Chuyến chưa khởi hành; toàn bộ vé phải `ISSUED` (không chấp nhận vé đã dùng, no-show, đổi chuyến hoặc đã hủy).
- Một booking có tối đa một yêu cầu theo quan hệ OneToOne hiện tại; không tự tạo lại kể cả yêu cầu trước bị từ chối.
- Lý do bắt buộc, tối đa 2000 ký tự.

```http
GET /api/cancel-requests?status=PENDING&bookingId=123&page=0&size=20
GET /api/cancel-requests/{id}
```

Không truyền status: lấy tất cả trạng thái. Các trạng thái hợp lệ: `PENDING`, `APPROVED`, `REJECTED`. Kết quả có thông tin khách/booking/chuyến, tổng tiền booking, vé tóm tắt, lý do gửi, lý do từ chối, người và thời điểm xử lý.

```http
PATCH /api/cancel-requests/{id}/resolve
```

Duyệt:
```json
{"isApproved": true}
```

Từ chối:
```json
{"isApproved": false, "rejectReason": "Không đáp ứng điều kiện hủy"}
```

- `isApproved` bắt buộc; từ chối bắt buộc rejectReason, tối đa 2000 ký tự.
- Người xử lý lấy từ token, không nhận staffId từ client và không dùng UUID giả.
- Khóa dòng request/booking khi xử lý trong transaction; request đã xử lý trả 409.
- Duyệt kiểm tra lại điều kiện hủy rồi đổi booking và toàn bộ vé sang `CANCELLED`.
- Từ chối giữ nguyên booking/vé, lưu lý do từ chối riêng với lý do khách gửi.
- **Không gọi VNPay hoàn tiền, không đổi Payment thành REFUNDED.** Yêu cầu duyệt hủy không phải bằng chứng đã hoàn tiền.

## 7. Báo cáo doanh thu

```http
GET /api/admin/statistics/revenue?from=2026-10-01&to=2026-10-31&groupBy=DAY
GET /api/admin/statistics/revenue?from=2026-01-01&to=2026-12-31&groupBy=MONTH
```

- `from`/`to`: `yyyy-MM-dd`, tính cả hai ngày; truy vấn từ đầu ngày from đến trước đầu ngày sau to.
- Mặc định đầu tháng đến hôm nay (hoặc đầu tháng của to nếu chỉ truyền to); khoảng tối đa 3660 ngày.
- `groupBy`: `DAY` (mặc định) hoặc `MONTH`.
- Dùng **ngày tạo bản ghi thanh toán `Payment.createdAt`**, không dùng ngày khởi hành hoặc booking.createdAt. Đây là timestamp sẵn có, chưa có trường settlement time riêng.
- Chỉ cộng Payment `SUCCESS`, gồm VNPay và thanh toán tại quầy; không cộng lại booking.totalPrice. Tiền dùng BigDecimal, đơn vị VND.
- Tổng hợp tại PostgreSQL, không tải toàn bộ giao dịch về Java.

Các trường response:
- `grossRevenue`: tổng tiền của các giao dịch SUCCESS.
- `successfulPayments`: số giao dịch SUCCESS.
- `series`: doanh thu/số giao dịch theo ngày hoặc tháng, chỉ có kỳ có giao dịch.
- `byPaymentMethod`: tổng theo phương thức thanh toán.
- `cancelledBookingReceipts`: phần tiền SUCCESS thuộc booking hiện đã CANCELLED, **không phải tiền đã hoàn**.

Chưa có sổ giao dịch hoàn tiền trong model hiện tại nên không công bố doanh thu ròng hay giả lập khoản hoàn tiền. Giao dịch SUCCESS của booking bị hủy vẫn thuộc grossRevenue, được tách ra ở cancelledBookingReceipts.

## 8. Cập nhật DB và kiểm thử

Entity CancelRequest thêm cột nullable `reason`, mở rộng `reject_reason` lên 2000 ký tự. Với môi trường dev có `ddl-auto=update`, Hibernate cập nhật schema. Nếu quản lý migration thủ công, xem `docs/sql/cancel-request-management.sql` và backup DB trước khi áp dụng. Không chạy seeder để cập nhật schema: seeder hiện có thao tác TRUNCATE dữ liệu.

Test độc lập, không kết nối DB/Redis/OpenAI:

```powershell
$env:JAVA_HOME = 'C:\Users\Admin\.jdks\ms-17.0.20.1'
.\mvnw.cmd clean test '-Dtest=!*Nhom01WwwApplicationTests'
```

Test mới kiểm tra validation, quyền qua method-security thật, owner booking, chống xử lý lặp, hủy sau khởi hành/vé đã dùng, trạng thái khóa, mật khẩu và tổng hợp báo cáo. Query PostgreSQL/migration và toàn bộ quy trình trên hạ tầng thật cần kiểm thử tích hợp riêng; unit test không thay thế bước đó.

Để chạy ứng dụng, cần tự tạo `src/main/resources/application.yml` theo file mẫu (file local được gitignore), cấu hình đúng PostgreSQL/Redis/JWT/email/OpenAI/VNPay. Không đưa thông tin bí mật lên Git.
