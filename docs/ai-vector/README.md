# AI Vector — Quick Start

Module `aivector` lập chỉ mục ngữ nghĩa cho `Route`, `Trip`, `Vehicle` bằng OpenAI embedding và PostgreSQL pgvector. Mục tiêu là tìm ngữ nghĩa có kiểm soát hướng tuyến, không thay thế logic đặt vé, trạng thái chuyến hay kiểm tra ghế.

> **CẬP NHẬT KIẾN TRÚC MỚI:** Toàn bộ hệ thống hiện đã hợp nhất vào **Shared Docker & Unified Database** (`busbooking` trên `postgres_db_local` với pgvector 0.8.7). Xem hướng dẫn chi tiết tại [Tài liệu Hạ tầng Hợp nhất](../infrastructure/SHARED-DOCKER-DATABASE.md) và [Kế hoạch Decommission](../infrastructure/DECOMMISSION-PLAN.md).

## Mục lục

- [Kiến trúc](01-ARCHITECTURE.md)
- [Cấu hình](02-CONFIGURATION.md)
- [Database và ingestion](03-DATABASE-AND-INGESTION.md)
- [Semantic Search](04-SEMANTIC-SEARCH.md)
- [Testing](05-TESTING-GUIDE.md)
- [Troubleshooting](06-TROUBLESHOOTING.md)
- [Bàn giao developer](07-DEVELOPER-HANDOVER.md)
- [Shared Docker & Database Architecture](../infrastructure/SHARED-DOCKER-DATABASE.md)

## 1. Kiểm tra công cụ

Mục đích: xác nhận Java 17, Docker và Maven Docker trước khi dùng module.

```powershell
java -version
docker version
docker compose version
```

Kết quả mong đợi: Java 17 và Docker daemon hoạt động.

## 2. Chọn môi trường đúng (Unified Runtime Database)

| Môi trường | Mục đích | Container / Database | Có được reset? |
|---|---|---|---|
| **Development Runtime (Dùng chung)** | Website, Chatbot, AI Vector, Demo Payment | `postgres_db_local` / `busbooking` | **KHÔNG** (Dữ liệu thật nghiệp vụ) |
| OpenAI acceptance riêng (Staging cũ) | Test cách ly harness acceptance | `postgres_openai_it` / `busbooking_openai_it` | Có, chỉ bằng test harness |
| Fake staging cũ | Vector deterministic mẫu | `nhom01_ai_vector_it_pg` / `busbooking_it` | Không |

## 3. Khởi động PostgreSQL chính với pgvector

Database chính `postgres_db_local` đã được cấu hình sẵn trong `docker-compose.yml` với image `pgvector/pgvector:0.8.7-pg16-bookworm`:

```powershell
docker compose up -d postgres_db_local redis_local
```

Kiểm tra kết nối và pgvector:
```powershell
docker exec postgres_db_local psql -U postgres -d busbooking -c "SELECT extname, extversion FROM pg_extension WHERE extname = 'vector';"
```

Kết quả: `vector | 0.8.7`.

## 4. Cấu hình API key an toàn

Mục đích: để Spring AI đọc key từ environment hoặc file cấu hình bí mật, không hard-code trong git.

```powershell
if ([string]::IsNullOrWhiteSpace($env:OPENAI_API_KEY)) {
  Write-Warning 'OPENAI_API_KEY is not set in this session. Spring will fallback to application config if present.'
}
```

## 5. Chạy migrations

Các migrations 001–004 đã được áp dụng trực tiếp vào database `busbooking`:
- `001-enable-pgvector.sql`
- `002-create-vector-store.sql`
- `003-create-vector-ingestion-manifest.sql`
- `004-create-vector-collection-metadata.sql`

## 6. Kiểm tra dữ liệu nghiệp vụ thật

Database chung `busbooking` đã có sẵn đầy đủ dữ liệu nghiệp vụ thật:
```powershell
docker exec postgres_db_local psql -U postgres -d busbooking -c `
  "SELECT (SELECT count(*) FROM routes) AS routes, (SELECT count(*) FROM trips) AS trips, (SELECT count(*) FROM vehicles) AS vehicles;"
```
Kết quả thực tế: 83 routes, 50,647 trips, 20 vehicles.

## 7. Chạy Ingestion và Semantic Search

Khi kích hoạt profile `ai-vector`, hệ thống đọc/ghi bảng `vector_store` ngay trong database `busbooking`.
Không copy fake vector sang database chính. Vector Ingestion chỉ chạy qua admin job hoặc service được cấp phép.
