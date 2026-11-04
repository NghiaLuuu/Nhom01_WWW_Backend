# AI Ticket Cancellation Assistant — Nhom01_WWW

## 1. Giới thiệu tổng quan
Hệ thống **AI Ticket Cancellation Assistant** là tính năng mở rộng của Chatbot hỗ trợ khách hàng trong nền tảng bán vé xe khách trực tuyến `Nhom01_WWW`. Tính năng cho phép khách hàng tra cứu, xem trước chi phí hủy vé / hoàn tiền theo chính sách quy định và thực hiện hủy vé an toàn ngay trên giao diện chatbot bằng ngôn ngữ tự nhiên.

Hệ thống hoạt động trên nền tảng **Spring Boot 4.1.1**, **Spring AI 2.0.0**, **PostgreSQL** kết hợp giải pháp thanh toán và hoàn tiền mô phỏng an toàn (**Demo Internal Refund**), đảm bảo tuyệt đối không can thiệp dòng tiền ngân hàng thực tế.

---

## 2. Các tính năng cốt lõi
1. **Tra cứu đơn đặt vé theo ngữ cảnh người dùng (`findMyBookings`):**
   - Tự động lấy danh tính từ `SecurityContextHolder` (JWT Token), không tin tưởng `userId` do AI hoặc client tự cung cấp.
   - Tìm kiếm đơn đặt vé theo điểm đi, điểm đến, ngày khởi hành hoặc mã đơn đặt vé.
2. **Kiểm tra tính hợp lệ & Chính sách hoàn tiền (`CancellationPolicyService`):**
   - Khởi hành > 24 giờ: Phí hủy 10%, hoàn tiền 90%.
   - Khởi hành 12 - 24 giờ: Phí hủy 20%, hoàn tiền 80%.
   - Khởi hành 2 - 12 giờ: Phí hủy 50%, hoàn tiền 50%.
   - Dưới 2 giờ hoặc chuyến xe đã xuất phát: Từ chối hủy vé (0% hoàn tiền).
   - Đơn `PENDING_PAYMENT` (chưa thanh toán): Hủy giữ chỗ ngay lập tức, không tính phí, không hoàn tiền.
   - Vé ở trạng thái `USED` hoặc `CANCELLED`: Không cho phép hủy lại.
3. **Xem trước hủy vé an toàn (`previewCancellation`):**
   - Sinh `confirmationToken` (UUID) với thời gian hết hạn (TTL) 5 phút.
   - Hiển thị `CancellationPreviewCard` trên frontend gồm: Mã đơn, tuyến xe, ngày giờ, số ghế muốn hủy, tiền vé, phí phạt, số tiền hoàn và nút xác nhận.
4. **Hỗ trợ hủy một phần hoặc toàn bộ (`Partial vs Full Cancellation`):**
   - Khách hàng có thể hủy 1 vé cụ thể trong đơn có nhiều vé mà không ảnh hưởng tới các vé còn lại.
   - Đơn hủy một phần chuyển sang trạng thái `PARTIALLY_CANCELLED`, các vé còn lại giữ nguyên `ISSUED`.
   - Khi hủy hết tất cả vé, đơn chuyển sang trạng thái `CANCELLED`.
5. **Giải phóng ghế tức thì (`Seat Release`):**
   - Vé được chuyển sang trạng thái `CANCELLED` ngay trong transaction.
   - Hàm truy vấn ghế `TicketRepository.findBookedSeatIdsByTripId()` loại trừ trạng thái `CANCELLED`, giúp các khách hàng khác có thể đặt lại ghế ngay lập tức.
6. **Bảo mật và chống gian lận (`Security & Anti-Cheat`):**
   - Chặn Prompt Injection thông qua bộ lọc ngữ nghĩa nghiêm ngặt.
   - Kiểm tra quyền sở hữu (`Ownership Check`): Khách hàng không thể tra cứu hoặc hủy đơn của người khác.
   - Chống Double-Click / Race Condition bằng Cache kết quả theo `confirmationToken` và Transaction Lock.
   - Token chỉ dùng được 1 lần và hết hạn sau 5 phút (trả về `410 GONE` nếu quá hạn).

---

## 3. Cấu trúc thư mục mã nguồn
```
src/main/java/vn/edu/iuh/nhom01_www/chatbot/cancellation/
├── dto/
│   ├── CancellationEligibilityDto.java
│   ├── CancellationIntent.java
│   ├── CancellationPreviewDto.java
│   ├── CancellationResultDto.java
│   └── MyBookingSummaryDto.java
├── service/
│   ├── CancellationAssistantService.java
│   ├── CancellationIntentParser.java
│   ├── CancellationPolicyService.java
│   └── CancellationPreviewManager.java
└── tool/
    └── CancellationTools.java
```

---

## 4. Hướng dẫn trải nghiệm nhanh trên giao diện
1. Mở Chatbot ở góc phải màn hình frontend (`/`).
2. Gõ tin nhắn: *"Hủy giúp tôi 2 vé đi Đà Lạt tối mai"* hoặc click vào Quick Suggestion tương ứng.
3. Chatbot hiển thị danh sách đơn đặt vé thỏa mãn (`BookingLookupCard`). Nhấn chọn đơn muốn hủy.
4. Hệ thống kiểm tra điều kiện và trả về thẻ `CancellationPreviewCard` với đồng hồ đếm ngược 5 phút.
5. Nhấn **"Xác nhận hủy vé"** để thực hiện hủy và nhận tiền hoàn mô phỏng, hoặc nhấn **"Giữ lại vé"** nếu đổi ý.
