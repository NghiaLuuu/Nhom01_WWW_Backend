# Cấu hình

## Phiên bản đã dùng

- Java 17
- Spring Boot 4.1.1
- Spring AI 2.0.0
- OpenAI `text-embedding-3-small`, 1536 dimensions
- PostgreSQL 16, pgvector 0.8.7
- COSINE_DISTANCE, HNSW

`src/main/resources/application-ai-vector.yml` chỉ active ở profile `ai-vector`. Nó cấu hình bảng `public.vector_store`, dimensions 1536, HNSW/COSINE_DISTANCE và `initialize-schema: false`; migrations phải chạy trước.

## Bí mật

```yaml
spring.ai.openai.api-key: ${OPENAI_API_KEY}
```

Đặt key cho session PowerShell bằng cơ chế nội bộ của tổ chức; không ghi key vào YAML, Git, log hoặc command history. `application.yml`/`application.example.yml` cũng phải giữ placeholder này.

## Hostname theo nơi chạy

| Client | Host hợp lệ |
|---|---|
| App/psql chạy trên Windows | `localhost:6666` khi compose publish port |
| Maven Docker | `postgres_openai_it:5432` trên Docker network |
| Acceptance DB | `jdbc:postgresql://postgres_openai_it:5432/busbooking_openai_it` |

`localhost` trong Maven Docker là chính container Maven, không phải máy Windows. Không dùng `localhost:6666/busbooking` cho acceptance.

## Phân tách database

- Production: không chạy migration/reset/acceptance từ tài liệu này.
- Fake staging: `busbooking_it`; giữ nguyên vectors deterministic/fake.
- OpenAI staging: `busbooking_openai_it`; riêng cho controlled re-index và real acceptance.

