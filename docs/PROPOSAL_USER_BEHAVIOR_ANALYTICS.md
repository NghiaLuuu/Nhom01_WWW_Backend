# Kế Hoạch Hệ Thống Tracking & Phân Tích Hành Vi Khách Hàng (Backend)

> **Nhánh:** `feature/user-behavior-analytics`  
> **Mục tiêu:** Thu thập, lưu trữ và tổng hợp phân tích hành vi người dùng (thời gian ở lại từng trang, hành động click, luồng đặt vé, tỷ lệ thoát trang) kết hợp đồng bộ với Frontend.

---

## 1. Mục Tiêu Nghiệp Vụ

1. **Đo lường thời gian xem trang (Page Dwell Time):** Ghi nhận người dùng ở lại trang nào lâu nhất (Trang chủ, Tìm kiếm chuyến xe, Chi tiết chuyến xe, Trang thanh toán).
2. **Theo dõi luồng hành vi (User Journey / Funnel Tracking):**
   - Tìm kiếm chuyến xe (`SEARCH_TRIPS`) -> Xem sơ đồ ghế (`VIEW_SEATS`) -> Chọn ghế (`SELECT_SEAT`) -> Nhập thông tin (`FILL_INFO`) -> Hoàn tất thanh toán (`BOOK_SUCCESS`) hoặc Bỏ dở giữa chừng (`ABANDON_BOOKING`).
3. **Phân tích người dùng vãng lai vs đã đăng nhập:**
   - Sử dụng `visitorId` (lưu dài hạn qua browser) + `sessionId` (theo từng phiên làm việc) kết hợp `userId` nếu người dùng đăng nhập.
4. **Cung cấp API Dashboard Thống Kê (Admin Analytics):**
   - Báo cáo số lượt xem trang theo thời gian thực / theo ngày.
   - Thống kê thời gian dừng lại trung bình theo từng route / URL.
   - Báo cáo các từ khóa tìm kiếm tuyến đường phổ biến nhất.
   - Tỷ lệ chuyển đổi phễu đặt vé.

---

## 2. Thiết Kế Kỹ Thuật (Backend)

### 2.1. CSDL: Entity `UserActivityLog`
Tách biệt hoàn toàn với `audit_logs` (vốn dùng cho bảo mật và auditing dữ liệu CRUD) để tối ưu throughput ghi dữ liệu lớn:

```java
@Entity
@Table(name = "user_activity_logs", indexes = {
    @Index(name = "idx_activity_session", columnList = "session_id"),
    @Index(name = "idx_activity_visitor", columnList = "visitor_id"),
    @Index(name = "idx_activity_created_at", columnList = "created_at"),
    @Index(name = "idx_activity_action", columnList = "action_type")
})
public class UserActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String sessionId;
    private String visitorId;
    private UUID userId;            // null nếu là khách vãng lai
    private String userEmail;

    @Column(nullable = false, length = 50)
    private String actionType;      // PAGE_VIEW, DWELL_TIME, CLICK_EVENT, SEARCH_TRIP, FUNNEL_STEP

    private String pageUrl;
    private String pageTitle;
    private String referrer;

    private Long dwellTimeSeconds;  // Thời gian ở lại trang (giây)

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> eventData; // metadata: tripId, searchParams, buttonName...

    private String deviceInfo;
    private String ipAddress;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

### 2.2. REST API Endpoints

#### Ingestion Endpoints (Client gửi về):
- `POST /api/analytics/events/batch`: Tiếp nhận mảng các sự kiện từ Frontend gửi lên định kỳ hoặc khi unmount trang.
- Xử lý bất đồng bộ qua `@Async` hoặc Redis Buffer để đảm bảo response time < 50ms, không ảnh hưởng trải nghiệm khách hàng.

#### Admin Aggregation Endpoints:
- `GET /api/admin/analytics/overview?from=&to=`: Tổng quan lượt truy cập, người dùng hoạt động (DAU), thời gian trung bình trên app.
- `GET /api/admin/analytics/dwell-time-by-page`: Top trang người dùng ở lại lâu nhất / rời đi nhanh nhất.
- `GET /api/admin/analytics/funnel-conversion`: Thống kê tỷ lệ chuyển đổi từng bước trong luồng đặt vé.
- `GET /api/admin/analytics/search-trends`: Các tuyến đường / thời gian khởi hành được tìm kiếm nhiều nhất.

---

## 3. Checklist Triển Khai

- [x] Tạo entity `UserActivityLog` và repository tương ứng với các query thống kê JPQL/Native SQL.
- [x] Xây dựng DTO tiếp nhận sự kiện: `TrackEventDto`, `BatchTrackEventRequest`.
- [x] Xây dựng `AnalyticsService` hỗ trợ lưu log bất đồng bộ `@Async` và truy vấn dữ liệu báo cáo.
- [x] Xây dựng controller `AnalyticsTrackingController` (`/api/analytics/events`) và `AdminAnalyticsController` (`/api/admin/analytics`).
- [x] Viết thêm test case và kiểm thử hiệu năng khi tiếp nhận lượng lớn event.

### 3.1. Backend contract đã triển khai

- `POST /api/analytics/events/batch` nhận tối đa 100 event và trả HTTP `202 Accepted`.
- Event bắt buộc có `sessionId`, `visitorId` và `actionType`; `actionType` chỉ nhận các giá trị đã định nghĩa trong `TrackEventDto`.
- Analytics ingestion cho phép guest; email người dùng đăng nhập được lấy từ security context, không tin dữ liệu định danh do client gửi.
- Các endpoint `/api/admin/analytics/**` yêu cầu authority `VIEW_STATISTICS`.
- Các endpoint báo cáo yêu cầu `from` và `to` theo định dạng `yyyy-MM-dd`; khoảng thời gian tối đa 366 ngày.

### 3.2. Frontend đã tích hợp

- Tự tạo và lưu `visitorId` trong `localStorage`, `sessionId` trong `sessionStorage`.
- Tự động ghi `PAGE_VIEW` và `DWELL_TIME` khi chuyển route hoặc rời trang.
- Ghi funnel `SEARCH_TRIPS`, `VIEW_SEATS`, `SELECT_SEAT`, `FILL_INFO`, `BOOK_SUCCESS` và `ABANDON_BOOKING`.
- Thêm màn hình `/admin/reports` để xem tổng quan, funnel, dwell time và xu hướng tìm kiếm.

### 3.3. Kiểm thử đã thực hiện

- Backend: `mvnw.cmd test` thành công, 1 test không lỗi.
- Analytics: `AnalyticsTrackingPerformanceTests` kiểm tra validation, batch 100 event và 100 batch đồng thời; kết quả đo được p50 13ms, p95 15ms, p99 16ms trong môi trường test (p95 < 50ms).
- Frontend: `npm run build` thành công với TypeScript và Vite.
- Booking thực tế lưu `bookings` và `tickets`; màn hình tra cứu hỗ trợ lọc theo tuyến, ngày và mã chuyến.
