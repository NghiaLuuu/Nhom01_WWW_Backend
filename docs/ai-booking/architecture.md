# Kiến Trúc Hệ Thống AI Assisted Bus Booking

## 1. Giới thiệu tổng quan
Hệ thống AI Assisted Bus Booking là giải pháp tích hợp giữa Mô hình ngôn ngữ lớn (LLM - Spring AI 2.0.0), Semantic Vector Search (pgvector) và các dịch vụ nghiệp vụ cốt lõi (Core Business Services) của sàn bán vé xe khách VEXE. Hệ thống cho phép người dùng tìm kiếm chuyến xe, chọn chỗ ngồi và đặt vé hoàn toàn thông qua giao tiếp tự nhiên bằng tiếng Việt với mức độ an toàn và toàn vẹn dữ liệu cao nhất.

## 2. Sơ đồ kiến trúc tầng (Layered Architecture)

```
+-----------------------------------------------------------------------------------+
|                           CLIENT LAYER (Web / Mobile)                             |
|             POST /api/chatbot/ask   |   POST /api/chatbot/booking-assistant       |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                        SECURITY & FIREWALL LAYER                                  |
|   - JwtAuthenticationFilter & CurrentUserService (Xác thực danh tính khách hàng) |
|   - Security Firewall (Chống Prompt Injection, rò rỉ PII, bỏ qua xác nhận)       |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                     AI ORCHESTRATION & NLP PARSER LAYER                           |
|   - BookingAssistantService (Điều phối hội thoại đa lượt, Spring AI ChatClient)   |
|   - BookingIntentParser (Bóc tách thực thể: điểm đi, đến, ngày, giờ, số vé, ghế)  |
|   - VietnameseLocationNormalizer (Chuẩn hóa địa danh: Sài Gòn <-> TP.HCM)         |
|   - BookingDraftManager (Quản lý bản nháp đặt vé & Idempotency Key)               |
+-----------------------------------------------------------------------------------+
                                         |
                  +----------------------+----------------------+
                  | (Semantic Discovery)                        | (Tool Calling Execution)
                  v                                             v
+-----------------------------------+     +-----------------------------------------+
|     AI VECTOR & RAG LAYER         |     |          BOOKING TOOLS ADAPTER          |
|  - PgVectorSearchService          |     |  - searchAvailableTrips                 |
|  - VectorStore (PostgreSQL 1536d) |     |  - getTripDetails                       |
|  - Directional Metadata Filter    |     |  - getAvailableSeats                    |
+-----------------------------------+     |  - calculateBookingPrice                |
                                          |  - prepareBooking (Tạo Draft)           |
                                          |  - confirmBooking (Xác nhận đặt vé)     |
                                          |  - getBookingStatus                     |
                                          +-----------------------------------------+
                                                                |
                                                                v
+-----------------------------------------------------------------------------------+
|                       CORE DOMAIN BUSINESS SERVICES                               |
|   - BookingService / BookingServiceImpl                                           |
|   - TripBookingAvailabilityService (Kiểm tra giờ cắt bán vé: 30p web, 10p quầy)  |
|   - SeatHoldRedisService (Phân tán Redis lock giữ chỗ ghế: 5 phút TTL)            |
|   - TicketRepository & SeatRepository (Kiểm tra trạng thái ghế thực tế trong DB)  |
|   - TripRepository & RouteRepository                                              |
+-----------------------------------------------------------------------------------+
```

## 3. Nguyên tắc thiết kế (Core Design Principles)
1. **Phân định ranh giới Vector Search và Transactional State**:
   - Vector Search chỉ sử dụng cho việc nhận biết ngữ nghĩa (tuyến đường, trạm xe, địa danh, tiện ích phương tiện).
   - Tuyệt đối **không** dùng Vector Embeddings để xác định số lượng ghế trống, giá vé hay trạng thái giao dịch hiện hành. Mọi dữ liệu kiểm tra tình trạng chỗ ngồi phải lấy từ database quan hệ và Redis khóa ghế trực tiếp.
2. **Human-in-the-loop (Xác nhận bắt buộc)**:
   - LLM không có quyền tự động gọi tool tạo đơn đặt vé khi người dùng chưa đồng ý rõ ràng.
   - Bắt buộc phải có bước tạo bản tóm tắt đặt vé (`prepareBooking`) để người dùng xem lại toàn bộ thông tin (tuyến, giờ chạy, số ghế, tổng tiền) và nói "Đồng ý" / "Xác nhận".
3. **Identity Isolation (Không tin tưởng ID do LLM truyền)**:
   - `customerId` được lấy độc quyền từ `SecurityContextHolder` thông qua `CurrentUserService`.
   - LLM không thể truyền `customerId` của người dùng khác để đặt hộ nếu không có cơ chế phân quyền hợp lệ.
4. **Resilience & Fallback First**:
   - Tích hợp cơ chế Fallback tự động (`BookingIntentParser` + deterministic execution) khi dịch vụ ChatModel/OpenAI gặp sự cố mạng hoặc timeout, đảm bảo quy trình đặt vé luôn thông suốt 100%.
