# Quy Trình Đặt Vé Tương Tác Qua AI (Booking Workflow)

## 1. Luồng nghiệp vụ chuẩn 8 bước (Mandatory 8-Step Flow)

```
[Người dùng]                 [AI Booking Assistant]              [Backend Services]               [Database / Redis]
     |                                 |                                  |                               |
 (1) | "Tìm 2 vé Sài Gòn đi Đà Lạt     |                                  |                               |
     |  ngày mai lúc 21h"              |                                  |                               |
     |-------------------------------->|                                  |                               |
     |                                 | searchAvailableTrips()           |                               |
     |                                 |--------------------------------->| Lọc chuyến còn chỗ & giờ hợp lệ|
     |                                 |                                  |------------------------------>|
     |                                 |<---------------------------------|                               |
 (2) | Trình bày danh sách chuyến      |                                  |                               |
     |<--------------------------------|                                  |                               |
     |                                 |                                  |                               |
 (3) | "Tôi chọn chuyến lúc 21h,       |                                  |                               |
     |  chọn 2 ghế tầng 1"             |                                  |                               |
     |-------------------------------->|                                  |                               |
     |                                 | getAvailableSeats(tripId)        |                               |
     |                                 |--------------------------------->| Lấy ghế chưa bán & chưa giữ   |
     |                                 |<---------------------------------|------------------------------>|
 (4) |                                 | prepareBooking(tripId, seatIds)  |                               |
     |                                 |--------------------------------->| Kiểm tra tình trạng ghế       |
     |                                 |                                  | Tạo IdempotencyKey & Draft    |
     |                                 |<---------------------------------| Lưu draft vào Redis/Memory    |
 (5) | Hiển thị Bản tóm tắt đặt vé     |                                  |                               |
     | Yêu cầu người dùng xác nhận     |                                  |                               |
     |<--------------------------------|                                  |                               |
     |                                 |                                  |                               |
 (6) | "Tôi đồng ý" / "Xác nhận"       |                                  |                               |
     |-------------------------------->|                                  |                               |
 (7) |                                 | confirmBooking(key, trip, seats) |                               |
     |                                 |--------------------------------->| createBooking(customerId, ...) |
     |                                 |                                  | - Lưu Booking (PENDING_PAYMENT)|
     |                                 |                                  | - Redis hold ghế 5 phút (300s)|
     |                                 |                                  | - Lưu Tickets (PENDING)       |
     |                                 |<---------------------------------|------------------------------>|
 (8) | Thông báo đặt vé thành công     |                                  |                               |
     | Mã đặt vé, giữ chỗ 5 phút       |                                  |                               |
     | Hướng dẫn thanh toán VNPay      |                                  |                               |
     |<--------------------------------|                                  |                               |
```

## 2. Chi tiết từng trạng thái đặt vé

| Trạng thái Booking | Trạng thái Ticket | Ý nghĩa nghiệp vụ | Hành động tiếp theo |
| :--- | :--- | :--- | :--- |
| **`PENDING_PAYMENT`** | `PENDING` | Vé đã được tạo và ghế đang được giữ chỗ độc quyền trên Redis trong 5 phút (300s). Chưa thanh toán tiền. | Người dùng bấm link thanh toán VNPay để hoàn tất. |
| **`PAID`** | `ISSUED` | Người dùng đã thanh toán thành công qua cổng thanh toán hoặc giao dịch tại quầy POS. | Hệ thống sinh mã QR vé và gửi email xác nhận. |
| **`CANCELLED`** | `CANCELLED` | Đơn hàng bị hủy do: Người dùng chủ động hủy, hoặc quá 5 phút mà chưa thanh toán (job tự động giải phóng ghế). | Ghế được mở lại tự do cho hành khách khác đặt. |
| **`FAILED`** | N/A | Lỗi hệ thống hoặc xung đột ghế trong lúc xác nhận. | AI thông báo lỗi cụ thể và gợi ý người dùng chọn ghế khác. |

## 3. Quy định nghiêm ngặt về Xác nhận (Human-in-the-loop)
* Khi người dùng yêu cầu đặt vé, hệ thống chỉ dừng lại ở bước **Tạo bản tóm tắt (Draft)**.
* Chỉ khi nào tin nhắn của người dùng thể hiện ý chí đồng ý rõ ràng (`"Tôi đồng ý"`, `"Xác nhận"`, `"Đồng ý đặt"`, `"OK đặt đi"`), hệ thống mới thực thi bước xác nhận tạo booking.
* Nếu người dùng nói `"Hủy"`, `"Thôi không đặt nữa"`, `"Đổi ý"`: Draft sẽ bị xóa ngay lập tức, giải phóng mọi ý định đặt chỗ và thông báo hủy thành công.
