# Quy trình tiếp nhận và duyệt/từ chối yêu cầu hủy vé

## Phạm vi

- Backend mới: `cancelrequest/workflow/`; Java 17 class/record, Spring/JPA/Validation sẵn có, không thêm dependency hoặc Lombok.
- Tái sử dụng `CancelRequest`, `Booking`, `Ticket`, `Payment`, repository cũ và `CurrentUserService`.
- Không sửa entity/controller/service/repository cũ, không thay trạng thái booking thành `CANCEL_REQUESTED` trong khi chờ duyệt.
- Frontend mới: `src/features/cancellation-workflow/`; dùng lại `AdminPageLayout`, `DataTable`, modal, định dạng tiền, Axios và auth store hiện có.
- Ngoại lệ tích hợp đã được phép: chỉ thêm import/route trong `App.tsx`, mục menu trong `Sidebar.tsx`, và tiêu đề trong `AdminLayout/index.tsx`.
- Menu **Yêu cầu hủy vé**, đường dẫn `/admin/cancel-requests`. Trang Đơn đặt vé cũ giữ nguyên.

## Quy tắc nghiệp vụ

1. Yêu cầu áp dụng cho **toàn bộ booking**, không phải từng ghế, phù hợp `CancelRequest.booking` hiện là `@OneToOne`.
2. Mỗi booking chỉ có một yêu cầu, kể cả sau khi bị từ chối. Không tự xóa/reopen yêu cầu hoặc sửa quan hệ trong entity cũ.
3. Khách hàng chỉ gửi/xem yêu cầu của chính mình; UUID khách được lấy từ phiên đăng nhập, không nhận từ body.
4. Admin hoặc Staff có `MANAGE_TICKET`/`TICKET_MANAGE` được tiếp nhận thay khách, xem và xử lý yêu cầu. Tài khoản phải `ACTIVE`.
5. Tiếp nhận qua API mới/màn nhân viên cần lý do thật, tối đa 1000 ký tự. Riêng nút gửi trên trang Hồ sơ cũ không thu thập lý do: endpoint tương thích lưu nguồn `PROFILE` và lý do trống, không tự bịa nội dung.
6. Tiếp nhận/duyệt chỉ cho booking `PAID`, có payment `SUCCESS`, còn trước giờ khởi hành, chuyến chưa `RUNNING`/`COMPLETED`, không có vé đã dùng/đổi chuyến/no-show hoặc trạng thái không hợp lệ.
7. Một số vé đã hủy trước đó được giữ nguyên; cần còn ít nhất một vé `ISSUED`/`UNUSED`/`RESERVED` để áp dụng hủy toàn bộ đơn. `UNUSED` được hỗ trợ để tương thích vé cũ mà verifier hiện có cũng chấp nhận.
8. Chờ duyệt: booking/vé/thanh toán không thay đổi; vé vẫn có hiệu lực.
9. Duyệt: khóa booking → yêu cầu → vé, kiểm tra lại điều kiện, chuyển request `APPROVED`, booking/vé `CANCELLED` trong cùng transaction. Ghế không còn nằm trong truy vấn vé chiếm chỗ hiện có.
10. Từ chối: bắt buộc lý do, tối đa 255 ký tự theo cột `rejectReason` cũ (không đổi schema cũ); request `REJECTED`, không thay booking/vé/payment. Có thể từ chối đóng yêu cầu sau khi khách đã dùng vé/chuyến đã khởi hành.
11. Nhân viên xử lý lấy từ phiên đăng nhập; lưu thời điểm xử lý, lý do ban đầu và lý do từ chối/ghi chú riêng biệt.
12. Có token `version` cho màn xử lý; yêu cầu đã thay đổi/đã xử lý trả 409. Khóa hàng bảo vệ các thao tác đồng thời trong module mới.

**Không thực hiện hoàn tiền.** Module không đổi payment `SUCCESS` thành `FAILED`, không xóa payment, không gọi cổng thanh toán, không tạo trạng thái “đã hoàn tiền”. Báo cáo doanh thu hiện có sẽ thấy booking đã hủy nhưng khoản thu vẫn được giữ nguyên.

## Bảng phụ mới và dữ liệu cũ

`cancel_request_workflow_details` chứa lý do yêu cầu, nguồn/địa chỉ email người tiếp nhận, thời điểm gửi, ghi chú xử lý và version. Khóa chính cũng là FK tới `cancel_requests.id`.

- Khi dùng `spring.jpa.hibernate.ddl-auto=update`, Hibernate tạo bảng phụ từ entity mới lúc khởi động.
- Với môi trường quản lý schema thủ công, chạy **sau khi các bảng nghiệp vụ đã tồn tại**:
  `docker/postgres/migrations/005-create-cancellation-workflow-details.sql`.
- Không sửa bảng `cancel_requests` cũ.
- Yêu cầu pending từ luồng cũ vẫn được đọc; giá trị `rejectReason` cũ được coi là lý do yêu cầu khi còn pending và được sao lưu sang bảng phụ trước khi xử lý bằng module mới.
- Không bịa lý do gốc cho yêu cầu cũ đã xử lý nhưng không còn dữ liệu để khôi phục.
- Các API cũ vẫn giữ nguyên hành vi; bảo vệ/validation mới áp dụng cho các API workflow mới, không phải bản sửa toàn bộ luồng cũ.

## API nhân viên

Prefix `/api/admin/cancel-requests`, yêu cầu Admin hoặc Staff có quyền quản lý vé/đặt vé.

| Method | Path | Chức năng |
| --- | --- | --- |
| GET | `/` | Tìm kiếm/lọc/phân trang |
| GET | `/{requestId}` | Chi tiết, điều kiện duyệt, vé và thanh toán đối chiếu |
| GET | `/bookings/{bookingId}/eligibility` | Tra booking trước khi tiếp nhận |
| POST | `/` | Tiếp nhận thay khách: `{ bookingId, reason }` |
| PATCH | `/{requestId}/approve` | `{ version, confirm: true, note? }` |
| PATCH | `/{requestId}/reject` | `{ version, reason }` |

Danh sách nhận `keyword` (tối đa 200 ký tự), `status` (`PENDING`/`APPROVED`/`REJECTED`), `page` (từ 0), `size` (1–100). Bỏ `status` để xem pending; gửi `status=` để xem tất cả.
Tìm kiếm hỗ trợ ID yêu cầu/booking, tên/email/điện thoại khách và tuyến đường; escape ký tự wildcard và chuẩn hóa chữ hoa/thường tiếng Việt độc lập với database locale.

**ID trong API duyệt/từ chối là ID yêu cầu, không phải booking ID.**
Response `{ success, message, data }` chỉ chứa DTO; không serialize trực tiếp entity có liên kết người dùng/password hash. Tiền là decimal string.

## API khách hàng

Yêu cầu `ROLE_CUSTOMER`:

- `POST /api/cancellation-workflow/requests` — `{ bookingId, reason }`.
- `GET /api/cancellation-workflow/my-requests` — lịch sử của tài khoản hiện tại.
- `GET /api/cancellation-workflow/my-requests/{requestId}` — chỉ chi tiết của chính mình; yêu cầu không thuộc tài khoản trả 404.

Tận dụng nguyên nút gửi yêu cầu hủy trên trang Hồ sơ cũ bằng endpoint được bổ sung (trước đây chưa tồn tại):
`PATCH /api/bookings/{bookingId}/request-cancel`. Endpoint cũng bắt buộc `ROLE_CUSTOMER`, kiểm tra chính chủ, điều kiện hủy và chặn trùng.
Không sửa `ProfilePage.tsx`/`ticket.service.ts`. Trang Hồ sơ cũ vẫn hiển thị trạng thái booking, chưa có giao diện trạng thái request riêng; trong lúc pending booking giữ `PAID`, gửi trùng sẽ nhận 409.

## Kiểm thử

Unit/controller/security, không chạy ứng dụng hoặc seeders:

```powershell
.\mvnw.cmd "-Dtest=CancellationWorkflowServiceTest,CancellationSecurityTest,CancellationControllerTest" test
```

Integration opt-in dùng PostgreSQL riêng; test có tạo/xóa schema/fixture. URL chỉ được là loopback và database tên chính xác `busbooking_cancellation_it`, tuyệt đối không dùng database làm việc `busbooking`.

```powershell
.\mvnw.cmd "-Dtest=CancellationPostgresIntegrationTest" "-Dcancellation.it.enabled=true" "-Dcancellation.it.jdbc-url=jdbc:postgresql://127.0.0.1:15633/busbooking_cancellation_it" "-Dcancellation.it.username=YOUR_TEST_USER" "-Dcancellation.it.password=YOUR_TEST_PASSWORD" test
```

Test dùng Hibernate/Spring Data thật và transaction thật, không Spring Boot startup; kiểm tra pending giữ vé, approve/reject, quyền sở hữu, version, dữ liệu legacy, tìm kiếm và hai luồng đồng thời gửi/duyệt cùng yêu cầu.
Các kiểm tra khóa chỉ bảo đảm trong module mới; không thay đổi cơ chế đồng thời của các API cũ chưa dùng cùng quy tắc khóa.

Frontend:

```powershell
npm run build
.\node_modules\.bin\oxlint.cmd src/features/cancellation-workflow
node --experimental-strip-types --test src/features/cancellation-workflow/tests/utils.test.mjs
```

Khi Vite chạy ở cổng 5183:

```powershell
node src/features/cancellation-workflow/tests/browser-smoke.mjs http://127.0.0.1:5183
```

Smoke test dùng Chrome/Edge và `puppeteer-core` có sẵn; chặn toàn bộ request `/api/`, không gửi thao tác hủy vào backend thật.
Khởi động lại backend bằng cấu hình hiện tại để nạp module/bảng phụ mới; không tự chạy `--seed` trên database có dữ liệu cần giữ.
