# Thiết Kế Tool Calling Trong Spring AI 2.0.0

## 1. Cơ chế hoạt động của Tool Calling trong Spring AI
Trong Spring AI 2.0.0, các tool nghiệp vụ được khai báo dưới dạng các Spring Bean có các phương thức đánh dấu `@Tool` và các tham số mô tả bằng `@ToolParam`. Khi khởi tạo `ChatClient`, các tool được đăng ký thông qua:
```java
ChatClient chatClient = ChatClient.builder(chatModel)
        .defaultTools(bookingTools)
        .build();
```

Khi người dùng gửi câu hỏi, Spring AI tự động trích xuất schema của tool và truyền cho OpenAI Model dưới dạng Function Calling spec. Khi model quyết định cần tra cứu hoặc thao tác, callback sẽ tự động thực thi trên JVM của ứng dụng và trả kết quả ngược lại cho model để sinh câu trả lời tự nhiên.

## 2. Danh mục 7 Booking Tools

### 1. `searchAvailableTrips`
* **Mục đích**: Tìm kiếm các chuyến xe thực tế còn ghế trống theo điểm đi, điểm đến, ngày và giờ khởi hành.
* **Tham số**:
  - `departureLocation` (String, required): Điểm khởi hành (ví dụ: "Sài Gòn", "Hà Nội").
  - `arrivalLocation` (String, required): Điểm đến (ví dụ: "Đà Lạt", "Nha Trang").
  - `departureDate` (String, optional): Ngày khởi hành (YYYY-MM-DD).
  - `preferredTime` (String, optional): Khung giờ (sáng, trưa, tối, hoặc 21:00).
  - `vehicleType` (String, optional): Loại xe (Limousine, Giường nằm, Ghế ngồi).
* **Kết quả**: `List<TripSearchResultDto>` (Mã chuyến, giờ chạy, giá vé, số ghế trống thực tế, trạng thái mở bán).

### 2. `getTripDetails`
* **Mục đích**: Lấy thông tin chi tiết của một chuyến xe cụ thể theo `tripId`.
* **Tham số**:
  - `tripId` (Long, required): Mã chuyến xe.
* **Kết quả**: `TripSearchResultDto`.

### 3. `getAvailableSeats`
* **Mục đích**: Lấy danh sách các ghế ngồi/giường nằm thực tế còn trống của chuyến xe.
* **Tham số**:
  - `tripId` (Long, required): Mã chuyến xe.
* **Logic nghiệp vụ**:
  - Lấy tất cả ghế thuộc phương tiện: `seatRepository.findByVehicleId(...)`.
  - Lấy ghế đã mua trong DB: `ticketRepository.findBookedSeatIdsByTripId(tripId)`.
  - Lấy ghế đang bị giữ chỗ trên Redis: `seatHoldRedisService.getHeldSeatIdsByTripId(tripId)`.
  - Loại trừ toàn bộ các ghế đã bán hoặc đang giữ, trả về ghế khả dụng.
* **Kết quả**: `List<AvailableSeatDto>` (seatId, seatName, floor, status="AVAILABLE").

### 4. `calculateBookingPrice`
* **Mục đích**: Tính toán trước tổng số tiền thanh toán theo số lượng vé.
* **Tham số**:
  - `tripId` (Long, required): Mã chuyến xe.
  - `seatCount` (int, required): Số lượng vé.
* **Kết quả**: `Map<String, Object>` (đơn giá, số lượng, tổng tiền và định dạng tiền tệ VNĐ).

### 5. `prepareBooking`
* **Mục đích**: Chuẩn bị bản tóm tắt đặt vé (Draft) và kiểm tra sơ bộ tình trạng ghế. **Không tạo booking chính thức**.
* **Tham số**:
  - `tripId` (Long, required): Mã chuyến xe.
  - `seatIds` (List<Long>, required): Danh sách ID các ghế người dùng muốn đặt.
* **Logic**:
  - Kiểm tra cổng bán vé: `tripBookingAvailabilityService.ensureWebBookingOpen(trip)`.
  - Kiểm tra ghế thuộc xe và chưa có ai đặt/giữ.
  - Tạo `idempotencyKey` ngẫu nhiên bảo vệ chống trùng lặp.
  - Lưu draft vào `BookingDraftManager`.
* **Kết quả**: `BookingDraftDto`.

### 6. `confirmBooking`
* **Mục đích**: Chính thức xác nhận và tạo đơn đặt vé khi người dùng đồng ý.
* **Tham số**:
  - `tripId` (Long, required): Mã chuyến xe.
  - `seatIds` (List<Long>, required): Danh sách ID ghế.
  - `idempotencyKey` (String, optional): Khóa chống trùng lặp từ bước prepareBooking.
* **Bảo mật & Ràng buộc**:
  - Khách hàng bắt buộc phải đăng nhập (`CurrentUserService.getCurrentCustomer()`).
  - Kiểm tra khóa Idempotency: nếu đơn đã được tạo trước đó (do retry mạng), trả về kết quả cũ mà không tạo lại.
  - Gọi `BookingService.createBooking(customerId, tripId, seatIds)` với giao dịch ACID và khóa Redis 300s.
* **Kết quả**: `BookingConfirmationResultDto`.

### 7. `getBookingStatus`
* **Mục đích**: Tra cứu tình trạng hiện tại của đơn đặt vé.
* **Tham số**:
  - `bookingId` (Long, required): Mã đơn đặt vé.
* **Kết quả**: `Map<String, Object>` (mã đặt vé, trạng thái PENDING_PAYMENT / PAID / CANCELLED, thời gian tạo, tổng tiền, danh sách ghế).
