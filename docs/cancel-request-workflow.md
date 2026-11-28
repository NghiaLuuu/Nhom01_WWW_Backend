# Quy trình tiếp nhận và xử lý yêu cầu hủy vé

## Giao diện

- Nhân viên/Admin: **Nghiệp Vụ → Yêu Cầu Hủy Vé**, đường dẫn `/admin/cancel-requests`.
- Có thể mở từ liên kết **Xem yêu cầu hủy** trong danh sách Đơn Đặt Vé; truyền `requestId`, không phải bookingId.
- Khách hàng: **Hồ sơ → Lịch sử đặt vé → Yêu cầu hủy vé**. Nhập lý do và xác nhận; gửi thành công sẽ chuyển sang tab **Yêu cầu hủy vé**.
- Trang nhân viên có bộ lọc trạng thái, mã booking (phần số của BKG-...), phân trang, kích thước trang và nút làm mới. Mặc định hiển thị yêu cầu chờ xử lý.
- Chi tiết có khách hàng/điện thoại, tuyến/chuyến/biển số, giờ khởi hành, ghế, mã vé, giá vé, tổng tiền đơn, lý do, người và thời điểm xử lý.

## Trạng thái và điều kiện

`PENDING → APPROVED` hoặc `PENDING → REJECTED`. Yêu cầu đã xử lý không được xử lý lại.

- Chỉ chủ booking có role CUSTOMER được gửi yêu cầu.
- Booking phải PAID, chuyến chưa khởi hành, tất cả vé phải ISSUED.
- Lý do gửi/từ chối tối đa 2000 ký tự; không chấp nhận chỉ có khoảng trắng.
- Giữ chính sách hiện tại: mỗi booking có một yêu cầu duy nhất, kể cả khi bị từ chối. Không xóa/ghi đè lịch sử để gửi lại.
- Duyệt: kiểm tra lại điều kiện trên máy chủ, hủy booking và toàn bộ vé trong cùng transaction.
- Từ chối: giữ nguyên booking/vé, lưu lý do từ chối tách biệt với lý do khách gửi.
- Duyệt hủy **không tự hoàn tiền**, không đổi payment SUCCESS thành REFUNDED. Phải đối soát/hoàn tiền bằng nghiệp vụ riêng.
- Khách hàng không được gọi các API hủy vé trực tiếp `/api/tickets/{id}/cancel` và `/api/tickets/{id}/no-show/cancel` để bỏ qua bước duyệt.

## API

| API | Quyền | Chức năng |
|---|---|---|
| POST `/api/cancel-requests` | CUSTOMER | Gửi `{bookingId, reason}` |
| GET `/api/cancel-requests/mine?status=&page=0&size=20` | CUSTOMER | Danh sách của chính khách hàng; bỏ status nếu không lọc |
| GET `/api/cancel-requests/mine/{id}` | CUSTOMER | Chi tiết chỉ của chủ yêu cầu; khác chủ trả 404 |
| GET `/api/cancel-requests?status=PENDING&bookingId=123&page=0&size=20` | Admin hoặc TICKET_MANAGE/MANAGE_TICKET | Hàng đợi và lịch sử xử lý |
| GET `/api/cancel-requests/{id}` | Như trên | Chi tiết mới nhất |
| PATCH `/api/cancel-requests/{id}/resolve` | Như trên | `{isApproved:true}` hoặc `{isApproved:false,rejectReason:"..."}` |

Response dùng `{success,message,data}`. Danh sách nằm ở `data.content` với `page`, `size`, `totalElements`, `totalPages`.

DTO chi tiết bổ sung `customerPhone`, `departureLocation`, `arrivalLocation`, `licensePlate`, `bookingStatus`, `canApprove`; mỗi vé có `seatName`, `price`. `canApprove` chỉ hỗ trợ giao diện, không thay thế validation lúc xác nhận. Không trả password hoặc entity graph.

## Chống xử lý lặp và cập nhật cũ

- Giữ PESSIMISTIC_WRITE trên request và booking trong transaction xử lý.
- Khi duyệt, khóa các ticket theo thứ tự id rồi kiểm tra trạng thái; cập nhật soát vé nguyên tử không thể đi qua điều kiện ISSUED sau khi vé đã hủy.
- Người xử lý lấy từ tài khoản nhân viên hiện tại, không nhận staffId do client cung cấp.
- Frontend chặn gửi lặp, không đóng modal lúc đang xử lý, không báo thành công khi API lỗi.
- API trả 409 nếu trạng thái đã thay đổi: frontend tải lại chi tiết và danh sách, không giữ nút duyệt của bản ghi đã xử lý.
- Hủy request tải danh sách/chi tiết cũ khi bộ lọc hoặc lựa chọn thay đổi, tránh response cũ ghi đè dữ liệu mới.

## Kiểm tra

```powershell
.\mvnw.cmd test '-Dtest=CancelRequestServiceTest,AdminManagementControllerTest'
```

Test kiểm tra owner, phân quyền, input, xử lý lặp, trạng thái vé đã thay đổi, vé đã dùng, chuyến đã khởi hành, kết quả duyệt/từ chối và DTO không chứa thông tin bảo mật. Các test này không ghi vào database thực.

Kiểm thử tích hợp thủ công cần làm thêm trên dữ liệu thử nghiệm:

1. Khách A gửi một yêu cầu từ booking đủ điều kiện; khách B không thể xem/gửi thay.
2. Hai nhân viên mở cùng yêu cầu. Một người duyệt, người còn lại xác nhận: phải nhận 409, thấy kết quả mới nhất, không ghi đè người xử lý.
3. Từ chối yêu cầu khác với lý do; khách xem lại thấy đúng người/thời gian/lý do, booking và vé vẫn còn hiệu lực.
4. Soát vé hoặc qua giờ khởi hành sau khi nhân viên mở chi tiết nhưng trước khi xác nhận duyệt: API phải từ chối và tải lại trạng thái.
5. Payment giữ nguyên sau khi hủy; giao diện không khẳng định đã hoàn tiền.

Khởi động lại backend/frontend để nhận code mới. Không cần chạy seeder. Không dùng dữ liệu thật để kiểm thử thao tác hủy.
