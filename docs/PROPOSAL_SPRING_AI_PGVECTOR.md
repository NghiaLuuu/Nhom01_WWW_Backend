# Kế Hoạch Chuyển Đổi AI Sang Spring AI + pgvector (RAG Architecture)

> **Nhánh:** `feature/spring-ai-pgvector`  
> **Căn cứ tài liệu:** Chapter 7 - Spring AI: Connecting Enterprise Data & APIs with AI Models (Spring Boot + Spring Data + Spring Security)

---

## 1. Hiện Trạng & Vấn Đề Cần Giải Quyết

- **Hiện trạng:**
  - `ChatbotService` hiện tại sử dụng `ChatModel.call(prompt)` trực tiếp theo mô hình Text-to-SQL thủ công nhiều bước (Guardrail -> Routing -> Generate SQL -> Execute JDBC -> NLG).
  - Thiếu khả năng tìm kiếm ngữ nghĩa (Semantic Search). Nếu khách hàng tìm kiếm bằng ngôn ngữ tự nhiên như *"xe giường nằm êm đi chơi lễ 30/4"*, *"chính sách đổi trả vé khi bận việc"*, Text-to-SQL dễ thất bại hoặc sinh câu lệnh không đúng.
  - Phản hồi từ LLM đôi khi bị thô, cứng nhắc hoặc phụ thuộc vào câu lệnh thô ráp thay vì tận dụng trọn vẹn `ChatClient` fluent API, `PromptTemplate`, và `ChatMemory` của Spring AI.

- **Mục tiêu chuyển đổi:**
  - Triển khai **pgvector** trên PostgreSQL để lưu trữ và truy vấn vector embeddings của dữ liệu chuyến xe, nhà xe, địa điểm, chính sách.
  - Sử dụng kiến trúc **RAG (Retrieval-Augmented Generation)**: Ingestion (ETL) -> VectorStore similarity search (Cosine similarity) -> Augment Prompt -> ChatClient.
  - Tinh chỉnh Prompt Template & System Prompt với OpenAI API Key để câu trả lời mượt mà, chuyên nghiệp, tự nhiên và đúng ngữ cảnh nghiệp vụ đặt vé xe.
  - Kết hợp **Tool Calling (`@Tool`)** theo kiến trúc an toàn (Slide 16-18 trong Chapter 7) để tra cứu dữ liệu thời gian thực hoặc tạo lệnh đặt vé an toàn qua server context.

---

## 2. Thiết Kế Kiến Trúc (Theo Chapter 7)

```
[Khách hàng (Web / Chat Widget)]
          │
          ▼
   [ChatbotController] ──> (Spring Security Context: Email, Roles)
          │
          ▼
   [ChatClient + ChatMemory]
          │
    ┌─────┴──────────────────────────────┐
    ▼                                    ▼
[VectorStore (pgvector)]         [TripTools / BookingTools (@Tool)]
    │ Cosine Similarity                  │ Safe service execution
    ▼                                    ▼
[PostgreSQL: busbooking]        [TripService / BookingService]
```

### 2.1. Cấu hình CSDL & pgvector
- Thay thế image PostgreSQL trong `docker-compose.yml`:
  ```yaml
  image: pgvector/pgvector:pg16
  ```
- Kích hoạt extension pgvector:
  ```sql
  CREATE EXTENSION IF NOT EXISTS vector;
  ```

### 2.2. Dependencies Maven (`pom.xml`)
- Bổ sung `spring-ai-starter-vector-store-pgvector` (tương thích Spring AI 2.0.x / Milestone/Release).
- Cấu hình `spring.ai.vectorstore.pgvector` trong `application.yml` (kích thước vector, distance type là `COSINE`).

### 2.3. Quy trình RAG (Retrieval Augmented Generation)
1. **Ingestion (ETL pipeline):**
   - Đọc dữ liệu từ Database (Tuyến xe, Điểm đón trả, Loại xe, Tiện ích, Giá vé, Chính sách huỷ vé).
   - Biến đổi thành `Document` metadata & content.
   - Sử dụng `TokenTextSplitter` phân đoạn nội dung.
   - Gọi Embedding Model (`text-embedding-3-small` hoặc `text-embedding-ada-002`) lưu vào `VectorStore`.
2. **Query Pipeline:**
   - Khi nhận câu hỏi từ người dùng:
   - `VectorStore.similaritySearch(SearchRequest.query(userQuestion).withTopK(5))`
   - Đưa nội dung tài liệu tìm được vào context của `PromptTemplate`.
   - `ChatClient.prompt()` sinh câu trả lời tự nhiên, thân thiện bằng tiếng Việt.

### 2.4. Tool Calling & Safe Ordering Guardrails
- Định nghĩa các `@Tool`:
  - `searchTrips(origin, destination, date)`: Tra cứu lịch trình và số ghế trống thực tế.
  - `getBookingDetails(bookingCode)`: Lấy thông tin vé đã đặt của chính user.
  - `createDraftBooking(tripId, seatNumbers)`: Tạo đơn đặt vé tạm chờ xác nhận.

---

## 3. Checklist Thực Hiện

- [ ] Cập nhật `docker-compose.yml` dùng image `pgvector/pgvector:pg16`.
- [ ] Bổ sung starter pgvector vào `pom.xml`.
- [ ] Cấu hình `application.yml` cho OpenAI embeddings và pgvector.
- [ ] Viết `TripVectorService` để khởi tạo & đồng bộ dữ liệu vào `VectorStore`.
- [ ] Tái cấu trúc `ChatbotService` chuyển sang dùng `ChatClient` với `PromptTemplate` & RAG Advisor.
- [ ] Định nghĩa `TripTools` cho tool-calling an toàn.
- [ ] Kiểm thử tích hợp câu hỏi tra cứu vé, tìm kiếm mờ, và tư vấn lộ trình.
