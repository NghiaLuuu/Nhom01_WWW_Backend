# Module Admin: Nhân sự & khách hàng

## Phạm vi và cấu trúc

- Backend mới: `src/main/java/vn/edu/iuh/nhom01_www/user/adminmanagement/`.
- Frontend mới: `src/features/user-management/` trong repository frontend.
- Trang: `/admin/user-management`, menu **Nhân sự & khách hàng**; chỉ Admin được sử dụng.
- Trang `/admin/users`, API cũ, entity, repository interface, security config và schema được giữ nguyên.
- Chỉ ba file frontend cũ được bổ sung tích hợp: `App.tsx`, `layouts/AdminLayout/Sidebar.tsx`, `layouts/AdminLayout/index.tsx`.
- Backend dùng Java 17 class/record, thư viện JDK và Spring/JPA/Validation sẵn có; không thêm dependency hay Lombok vào module mới.

## Chức năng

- Nhân viên: tìm kiếm/lọc/phân trang từ database; xem, tạo, sửa thông tin; chọn vai trò và quyền bổ sung; khóa/mở khóa; đặt lại mật khẩu.
- Khách hàng: tìm kiếm/lọc/phân trang; xem thông tin; khóa/mở khóa; đặt lại mật khẩu.
- Không xóa cứng hoặc sửa lịch sử đặt vé. Không có dữ liệu giả hoặc nhánh giả lập thành công khi API lỗi.
- Quyền kế thừa từ vai trò được hiển thị riêng với quyền cấp thêm.
- Không thay đổi tài khoản Admin đang đăng nhập qua module này; sử dụng trang Hồ sơ cá nhân.
- Không khóa/hạ vai trò Admin đang hoạt động cuối cùng; kiểm tra với khóa ghi các hàng Admin.
- Không kích hoạt khách hàng đang chờ OTP qua thao tác mở khóa.

## API mới

Mọi endpoint yêu cầu JWT với `ROLE_ADMIN`, kể cả khi gọi trực tiếp không qua giao diện.
Prefix: `/api/admin/user-management`.

| Method | Đường dẫn | Chức năng |
| --- | --- | --- |
| GET | `/options` | Vai trò Admin/Staff và danh mục quyền |
| GET | `/staff` | Danh sách nhân viên |
| GET | `/staff/{id}` | Chi tiết nhân viên |
| POST | `/staff` | Tạo nhân viên |
| PUT | `/staff/{id}` | Sửa nhân viên |
| PATCH | `/staff/{id}/status` | Khóa/mở khóa nhân viên |
| PUT | `/staff/{id}/password` | Đặt lại mật khẩu nhân viên |
| GET | `/customers` | Danh sách khách hàng |
| GET | `/customers/{id}` | Chi tiết khách hàng |
| PATCH | `/customers/{id}/status` | Khóa/mở khóa khách hàng |
| PUT | `/customers/{id}/password` | Đặt lại mật khẩu khách hàng |

Danh sách nhận `keyword`, `status`, `page` (từ 0), `size` (1–100). Nhân viên có thêm `roleId`.
Response giữ định dạng `{ success, message, data }`. `data` danh sách gồm `content`, `number`, `size`, `totalElements`, `totalPages`.

Tạo nhân viên nhận `{ details, password }`. Sửa nhận `{ details, version }`.
`details` gồm `fullName`, `email`, `phoneNumber`, `dateOfBirth`, `staffCode`, `hireDate`, `position`, `workStation`, `roleId`, `permissions`.
Khóa/mở nhận `{ status: "ACTIVE" | "LOCKED", version }`. Đặt lại mật khẩu nhận `{ password, version }`.
`version` phải lấy từ response mới nhất; dữ liệu cũ trả HTTP 409 thay vì ghi đè.

## Mật khẩu và phiên đăng nhập

- API response mới không chứa mật khẩu/hash; request chứa mật khẩu được che trong `toString()`.
- Mật khẩu mới tối thiểu 8 ký tự, tối đa 72 byte UTF-8 để phù hợp BCrypt.
- Dùng `PasswordEncoder` hiện có; không dùng chung mật khẩu mặc định.
- Các thay đổi được lưu qua JPA `saveAndFlush`, cập nhật `@Version` mà JWT filter hiện có kiểm tra; refresh token của tài khoản bị thay đổi cũng được thu hồi.
- Đặt lại mật khẩu không gửi email tự động. Admin cần thông báo mật khẩu mới riêng cho chủ tài khoản.
- API cũ không được thay đổi: các giới hạn/bảo mật bổ sung ở trên chỉ áp dụng cho module mới.

## Kiểm tra mà không chạy seeder/database

Backend:

```powershell
.\mvnw.cmd "-Dtest=AdminUserManagementServiceTest,AdminUserManagementSecurityTest,UserManagementQueriesTest,AdminUserManagementControllerTest" test
```

Các test trên dùng mock hoặc method-security context, không khởi động ứng dụng, không kết nối database, không chạy seeder.

Frontend:

```powershell
npm run build
.\node_modules\.bin\oxlint.cmd src/features/user-management
```

Smoke test trình duyệt dùng `puppeteer-core` đã có trong dự án và Chrome/Edge cài sẵn. Khởi động Vite ở terminal riêng:

```powershell
npm run dev -- --host 127.0.0.1 --port 5181 --strictPort
```

Sau đó:

```powershell
node src/features/user-management/tests/browser-smoke.mjs http://127.0.0.1:5181
```

Toàn bộ request `/api/` trong smoke test bị chặn và giả lập; không ghi vào backend thật.
Test kiểm tra menu/quyền, phân trang, tạo/sửa/xem nhân viên, xác nhận mật khẩu, khóa/đặt lại mật khẩu, hủy thao tác, tìm kiếm rỗng, API lỗi/thử lại và màn hình nhỏ với sidebar thu gọn.

## Sử dụng thực tế

Khởi động lại backend theo cấu hình hiện tại để nạp controller mới; đăng nhập Admin rồi mở menu mới. Không cần migration.
Frontend tiếp tục dùng `VITE_API_URL` và Axios/auth store hiện có.
Các seeder tự chạy trong code cũ vẫn giữ nguyên; đừng dùng `--seed` trên database có dữ liệu cần giữ.
