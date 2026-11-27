# Tài Liệu Bàn Giao Kỹ Thuật (Developer Handover)

## 1. Thông tin chung
* **Dự án**: Sàn đặt vé xe trực tuyến VEXE (Backend Spring Boot).
* **Module**: AI Assisted Bus Booking (Spring AI 2.0.0 + Vector Search + Tool Calling).
* **Phiên bản Java**: 17.
* **Cơ sở dữ liệu**: PostgreSQL (với extension `pgvector`), Redis (Quản lý Seat Hold TTL 300s).

## 2. Các gói mã nguồn chính được bàn giao
1. **`vn.edu.iuh.nhom01_www.chatbot.booking.dto`**:
   - `BookingIntent.java`: Chứa các thực thể bóc tách từ ngôn ngữ tự nhiên (điểm đi, điểm đến, ngày, giờ, số lượng vé, loại xe, ghế mong muốn, cờ xác nhận/hủy).
   - `TripSearchResultDto.java`: Dữ liệu chuyến xe thực tế trả về cho người dùng và LLM.
   - `AvailableSeatDto.java`: Thông tin ghế trống thực tế.
   - `BookingDraftDto.java`: Bản tóm tắt đặt vé kèm `idempotencyKey` và thời hạn 10 phút.
   - `BookingConfirmationResultDto.java`: Kết quả tạo đơn booking chính thức (`PENDING_PAYMENT`).
2. **`vn.edu.iuh.nhom01_www.chatbot.booking.tool`**:
   - `BookingTools.java`: Bộ 7 công cụ tương thích Spring AI 2.0.0 (`@Tool` và `@ToolParam`).
3. **`vn.edu.iuh.nhom01_www.chatbot.booking.service`**:
   - `BookingAssistantService.java`: Điều phối hội thoại đa lượt, tích hợp `ChatClient` và fallback.
   - `BookingDraftManager.java`: Quản lý bộ nhớ đệm bản nháp đặt vé và kết quả chống trùng lặp (Idempotency).
   - `BookingIntentParser.java`: Bộ phân tích ngữ nghĩa và trích xuất thực thể tiếng Việt định danh.
4. **`vn.edu.iuh.nhom01_www.chatbot.controller`**:
   - `ChatbotController.java`: Mở rộng thêm endpoint `POST /api/chatbot/booking-assistant`.
5. **`vn.edu.iuh.nhom01_www.aivector`**:
   - `VietnameseLocationNormalizer.java`: Mở rộng quyền truy cập `public` và bổ sung các alias mở rộng ("TP. Hồ Chí Minh", "tp ho chi minh", "Sài Gòn").
6. **`src/test/java/vn/edu/iuh/nhom01_www/chatbot/booking`**:
   - `BookingAssistantTest.java`: Bộ 15 bài kiểm thử tự động bao phủ 100% các kịch bản bắt buộc.

## 3. Ranh giới an toàn & Quy tắc bảo tồn mã nguồn
* **Module AI Vector**: Không bị thay đổi hay phá vỡ logic gốc, các bài test `AiVectorConfigurationTest` và `PgVectorSearchServiceTest` tiếp tục chạy hoàn hảo.
* **Core Booking Service**: Tái sử dụng trọn vẹn `BookingService.createBooking(...)`, không viết lại hay bypass logic nghiệp vụ giữ chỗ trên Redis và cơ sở dữ liệu.
* **Bảo mật**: Người dùng nặc danh không thể đặt vé thành công; chỉ có tài khoản đăng nhập hợp lệ mới được cấp phép tạo đơn đặt chỗ.
* **Git Commit**: Không thực hiện `git commit`, `git push` hay `git merge` theo đúng quy định phân công nhiệm vụ.

## 4. Kết luận mức độ sẵn sàng (Production Readiness)
* Mã nguồn biên dịch: **PASS** (100%).
* Kiểm thử tự động (Unit & Integration): **29/29 tests PASS** (100%).
* Tài liệu thiết kế, hướng dẫn vận hành và API: **ĐẦY ĐỦ** tại `docs/ai-booking/`.
* **Trạng thái**: Đã sẵn sàng bàn giao và đưa lên môi trường Staging / Pre-production.
