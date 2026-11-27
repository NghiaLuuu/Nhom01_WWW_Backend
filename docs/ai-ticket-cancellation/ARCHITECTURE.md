# Kiến trúc hệ thống AI Hỗ trợ hủy vé (AI Ticket Cancellation Architecture)

## 1. Sơ đồ luồng xử lý tổng thể

```mermaid
sequenceDiagram
    autonumber
    actor User as Khách hàng (Chatbot UI)
    participant ChatCtrl as ChatbotController / API
    participant Assist as CancellationAssistantService
    participant Parser as CancellationIntentParser
    participant Tools as CancellationTools (Spring AI @Tool)
    participant Policy as CancellationPolicyService
    participant PrevMgr as CancellationPreviewManager
    participant BookSvc as BookingService & CancelRequestService
    participant DB as PostgreSQL Database

    User->>ChatCtrl: "Hủy giúp tôi 2 vé đi Đà Lạt tối mai"
    ChatCtrl->>Assist: assistCancellation(request, currentUser)
    Assist->>Parser: parseIntent(message)
    Parser-->>Assist: CancellationIntent (Action: LOOKUP_BOOKINGS)
    
    Assist->>Tools: findMyBookings(destination="Đà Lạt", ...)
    Tools->>DB: Query Bookings by CustomerId & filters
    DB-->>Tools: List<Booking>
    Tools-->>Assist: List<MyBookingSummaryDto>
    Assist-->>ChatCtrl: ChatbotResponse (BOOKING_LOOKUP, candidate bookings)
    ChatCtrl-->>User: Hiển thị BookingLookupCard

    User->>ChatCtrl: Click "Chọn hủy đơn #101"
    ChatCtrl->>Assist: assistCancellation("HỦY ĐƠN 101", currentUser)
    Assist->>Tools: previewCancellation(bookingCode="BK101", seatNumbers=[])
    Tools->>Policy: evaluateEligibility(booking, seats)
    Policy-->>Tools: CancellationEligibilityDto (hours, fee %, refund amount)
    Tools->>PrevMgr: createPreview(previewDto) (UUID token, 5-min TTL)
    PrevMgr-->>Tools: confirmationToken
    Tools-->>Assist: CancellationPreviewDto
    Assist-->>ChatCtrl: ChatbotResponse (CANCELLATION_PREVIEW)
    ChatCtrl-->>User: Hiển thị CancellationPreviewCard (Ticker 5:00)

    User->>ChatCtrl: Click "Xác nhận hủy vé" (Gửi token)
    ChatCtrl->>Assist: assistCancellation("CONFIRM_CANCELLATION:token", currentUser)
    Assist->>Tools: confirmCancellation(confirmationToken)
    Tools->>PrevMgr: validateAndConsumeToken(token)
    PrevMgr-->>Tools: Stored Preview Context
    Tools->>BookSvc: cancelTicket / cancelBooking (@Transactional)
    BookSvc->>DB: Update Ticket status -> 'CANCELLED'
    BookSvc->>DB: Update Booking status -> 'CANCELLED' / 'PARTIALLY_CANCELLED'
    BookSvc->>DB: Insert CancelRequest & Demo Refund record
    BookSvc-->>Tools: CancellationResultDto
    Tools-->>Assist: Result
    Assist-->>ChatCtrl: ChatbotResponse (CANCELLATION_SUCCESS)
    ChatCtrl-->>User: Hiển thị CancellationSuccessCard
```

---

## 2. Nguyên tắc bảo mật và giải phóng ghế

### 2.1. Xác thực danh tính độc lập (Zero-Trust AI Identity)
- AI Model hoàn toàn không có quyền tự chuyển trạng thái cơ sở dữ liệu. Mọi thay đổi nghiệp vụ đều bắt buộc phải thông qua backend Java service.
- Định danh khách hàng được trích xuất trực tiếp từ `SecurityContextHolder.getContext().getAuthentication().getName()`. Mọi payload gửi từ AI tool hoặc client chứa `userId` hay `customerId` lạ đều bị phớt lờ và ghi đè bằng user thực tế đang đăng nhập.

### 2.2. Kiểm soát đồng thời và Chống Double-Click (Concurrency & Idempotency)
- `CancellationPreviewManager` quản lý `confirmationToken` (UUID ngẫu nhiên, TTL 300 giây).
- Khi người dùng bấm nút xác nhận nhiều lần liên tiếp (Double-Click / Network Retry), phương thức `consumeToken(token)` sử dụng cơ chế atomic lookup & invalidation.
- Kết quả hủy lần đầu được lưu trữ tạm thời trong `idempotencyResultMap` (TTL 10 phút). Các request trùng token đến sau sẽ nhận ngay kết quả đã xử lý thành công trước đó mà không bao giờ thực thi hủy hai lần hay hoàn tiền kép.

### 2.3. Cơ chế giải phóng ghế tức thì (Instant Seat Release)
Hệ thống quản lý trạng thái ghế đặt trong [TicketRepository.java](file:///d:/WWW/Nhom01_WWW_Backend/src/main/java/vn/edu/iuh/nhom01_www/ticket/repository/TicketRepository.java):
```java
@Query("SELECT t.seat.id FROM Ticket t WHERE t.trip.id = :tripId AND t.status IN ('ISSUED', 'RESERVED', 'USED')")
List<Long> findBookedSeatIdsByTripId(@Param("tripId") Long tripId);
```
Khi vé được chuyển sang trạng thái `CANCELLED`:
- Trạng thái `CANCELLED` ngay lập tức bị loại khỏi danh sách ghế đã đặt.
- Không cần xóa bản ghi vé khỏi cơ sở dữ liệu (đảm bảo tính toàn vẹn kiểm toán và lưu vết lịch sử hoàn tiền).
- Khách hàng khác tìm kiếm chuyến xe sẽ thấy ghế trống ngay lập tức trên sơ đồ xe.
