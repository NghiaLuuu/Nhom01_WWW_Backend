# Cấu hình Module AI Vector

## Phiên bản đã dùng

- Java 17
- Spring Boot 4.1.1
- Spring AI 2.0.0
- OpenAI `text-embedding-3-small`, 1536 dimensions
- PostgreSQL 16.15, pgvector 0.8.7
- COSINE_DISTANCE, HNSW

Profile `ai-vector` được cấu hình trực tiếp trong `src/main/resources/application.yml` (thông qua multi-document YAML `---` kích hoạt theo `spring.config.activate.on-profile: ai-vector`). Cấu hình quản lý bảng `public.vector_store` trên database nghiệp vụ chung `busbooking`, dimensions 1536, HNSW/COSINE_DISTANCE và `initialize-schema: false`; migrations 001–004 đã được áp dụng.

## Bí mật API Key

```yaml
spring.ai.openai.api-key: ${OPENAI_API_KEY:${spring.ai.openai.api-key:}}
```

Hệ thống ưu tiên đọc từ biến môi trường `OPENAI_API_KEY`. Nếu không có, hệ thống sử dụng cấu hình mặc định an toàn trong môi trường phát triển.

## Hostname theo ngữ cảnh chạy

| Client | Host hợp lệ | Datasource URL |
|---|---|---|
| Ứng dụng chạy trên máy host (Windows / IDE) | `localhost:6666` | `jdbc:postgresql://localhost:6666/busbooking` |
| Container Backend chạy qua Docker Compose | `postgres_db_local:5432` | `jdbc:postgresql://postgres_db_local:5432/busbooking` |
| Acceptance Test harness cách ly | `postgres_openai_it:5432` | `jdbc:postgresql://postgres_openai_it:5432/busbooking_openai_it` |

## Kiến trúc Database Hợp nhất (Unified Architecture)

- **Development Runtime (Dùng chung):** Database `busbooking` trên container `postgres_db_local` đóng vai trò là single source of truth cho Website và AI Chatbot.
- **Phân tách an toàn:** Acceptance test harness `PgVectorOpenAiStagingAcceptanceTest` được gắn assertion bắt buộc ngăn chặn truy cập hoặc reset database chính `busbooking`.
