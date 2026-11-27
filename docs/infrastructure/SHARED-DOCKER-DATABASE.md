# Hợp nhất Hạ tầng: Shared Docker & Unified PostgreSQL Database

## 1. Giới thiệu tổng quan
Hệ thống `Nhom01_WWW` đã hoàn thành việc hợp nhất toàn bộ các module nghiệp vụ và AI vào một cơ chế **Shared Docker Compose** và **Unified Development Database**.

Toàn bộ các phân hệ:
1. **Website Bán vé xe trực tuyến (Website Booking)**
2. **AI Vector Search & Ingestion (`aivector`)**
3. **AI Booking Assistant (Hỗ trợ tìm chuyến & chọn ghế)**
4. **Demo Internal Payment (Thanh toán QR mô phỏng)**
5. **AI Ticket Cancellation (Hỗ trợ hủy vé & hoàn tiền ví demo)**
6. **Ticket Management (Soát vé QR, check-in, no-show)**

Hiện nay **đều đọc và ghi trên cùng một nguồn dữ liệu nghiệp vụ thật duy nhất**:
- **Database:** `busbooking`
- **Container:** `postgres_db_local` (Image: `pgvector/pgvector:0.8.7-pg16-bookworm`)
- **Port:** `0.0.0.0:6666 -> 5432`
- **Volume:** `nhom01_www_backend_postgres_data`
- **Internal Network:** `nhom01_www_backend_busbooking-network`

Không còn bất kỳ module runtime AI nào trỏ tới các database staging biệt lập (`busbooking_it`, `busbooking_openai_it`).

---

## 2. So sánh Kiến trúc Docker Trước & Sau Hợp nhất

### Trước hợp nhất (Phân mảnh 3 Container PostgreSQL)
```
[Website & Backend] --------> Container: postgres_db_local (postgres:16-alpine)
                              Database: busbooking (Chưa có extension vector)
                              Volume: nhom01_www_backend_postgres_data

[AI Integration Tests] -----> Container: nhom01_ai_vector_it_pg (pgvector)
                              Database: busbooking_it (Vectors giả / deterministic)
                              Volume: nhom01_ai_vector_it_pgdata

[AI Acceptance Tests] ------> Container: postgres_openai_it (pgvector)
                              Database: busbooking_openai_it (Staging vectors)
                              Volume: busbooking_openai_it_data
```

### Sau hợp nhất (Unified Shared Infrastructure)
```
  ┌────────────────────────────────────────────────────────────────────────┐
  │                           Nhom01_WWW System                            │
  │                                                                        │
  │  Website Frontend  │  Chatbot AI  │  Demo Payment  │  Ticket Scanner   │
  └──────────┬─────────────────┬──────────────┬───────────────┬────────────┘
             │                 │              │               │
             ▼                 ▼              ▼               ▼
  ┌────────────────────────────────────────────────────────────────────────┐
  │                 Spring Boot Backend (Port 8080)                        │
  │  BookingService | TripService | PaymentService | CancellationService   │
  │  PgVectorSearchService | VectorIngestionService | ChatbotService       │
  └───────────────────────────────────┬────────────────────────────────────┘
                                      │ JDBC (:6666 / :5432)
                                      ▼
  ┌────────────────────────────────────────────────────────────────────────┐
  │       Single Shared PostgreSQL Container (postgres_db_local)           │
  │       Image: pgvector/pgvector:0.8.7-pg16-bookworm                     │
  │       Database: busbooking                                             │
  │       Volume: nhom01_www_backend_postgres_data (Persistent)            │
  │       Extensions: vector (0.8.7), hstore, uuid-ossp                    │
  │                                                                        │
  │   - Bảng nghiệp vụ thật: routes, trips, seats, bookings, tickets, ...   │
  │   - Bảng vector ngữ nghĩa: vector_store, vector_collection_metadata,    │
  │                            vector_ingestion_manifest                   │
  └────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Thống nhất Dữ liệu Nghiệp vụ (Data Consistency)

Bảng kiểm chứng dữ liệu thật trong shared database `busbooking`:

| Bảng nghiệp vụ | Số lượng bản ghi thật | Vai trò chia sẻ giữa Website & AI |
|:---|:---:|:---|
| `routes` | 83 | Website tra cứu; AI chuẩn hóa địa danh & Semantic Search |
| `trips` | 50,647 | Chuyến xe thật; AI kiểm tra chuyến hợp lệ theo ngày giờ |
| `seats` | 1,773 | Sơ đồ ghế thật; AI và Website đọc cùng tình trạng ghế |
| `bookings` | 13,298 | Đơn đặt vé; Khách đặt qua web hay AI đều ghi chung bảng |
| `tickets` | 26,339 | Vé xe; AI hủy vé giải phóng ghế lập tức cho Website |
| `payments` | 10,921 | Lịch sử thanh toán Demo / VNPAY dùng chung |
| `customers` | 144 | Tài khoản khách hàng; Chatbot trích xuất từ JWT token |
| `vector_store` | Quản lý HNSW | Lưu vector embeddings 1536 chiều từ OpenAI |
| `vector_collection_metadata` | 1 | Lưu định danh mô hình (`text-embedding-3-small`, 1536D) |

---

## 4. Hướng dẫn Vận hành Môi trường Local

### 4.1. Khởi động PostgreSQL & Redis chung bằng Docker Compose
```powershell
# Chạy các container dịch vụ hạ tầng
docker compose up -d postgres_db_local redis_local
```

### 4.2. Khởi động Backend (Local Spring Boot)
Backend sẽ tự động kết nối vào PostgreSQL qua port `localhost:6666` và Redis qua `localhost:6379`:
```powershell
# Chạy Spring Boot thông thường
.\mvnw.cmd spring-boot:run

# Chạy với profile AI Vector (kích hoạt Semantic Search thật)
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=ai-vector"
```

### 4.3. Khởi động toàn bộ bằng Docker Compose (Full Stack)
```powershell
docker compose up -d
```

---

## 5. Quy trình Sao lưu & Phục hồi (Backup & Restore)

### 5.1. Sao lưu dữ liệu (Backup)
```powershell
# 1. Sao lưu logic (SQL dump)
docker exec postgres_db_local pg_dump -U postgres -d busbooking -f /tmp/busbooking_backup.sql
docker cp postgres_db_local:/tmp/busbooking_backup.sql ./backups/busbooking_backup_$(Get-Date -Format 'yyyyMMdd_HHmmss').sql

# 2. Sao lưu vật lý (Volume tar.gz)
docker run --rm -v nhom01_www_backend_postgres_data:/from:ro -v ${PWD}/backups:/to alpine `
  tar czf /to/nhom01_www_backend_postgres_data_backup_$(Get-Date -Format 'yyyyMMdd_HHmmss').tar.gz -C /from .
```

### 5.2. Phục hồi dữ liệu (Restore)
```powershell
# Phục hồi từ SQL dump vào database busbooking
docker cp ./backups/busbooking_backup.sql postgres_db_local:/tmp/restore.sql
docker exec postgres_db_local psql -U postgres -d busbooking -f /tmp/restore.sql
```

---

## 6. Cơ chế Bảo vệ Kiểm thử An toàn (Test Safety Guards)
1. **Hard-coded Acceptance Guard:** Test `PgVectorOpenAiStagingAcceptanceTest` được gắn assertion bắt buộc:
   `assertDedicatedAcceptanceDatabase(jdbcUrl)`. Nếu vô tình truyền JDBC URL của database dùng chung (`busbooking`), test sẽ lập tức quăng ngoại lệ `IllegalStateException` và dừng lại ngay, ngăn chặn triệt để nguy cơ ghi đè hay reset dữ liệu thật.
2. **Read-Only Verification:** Bộ test `UnifiedDatabaseDataVerificationTest` thực hiện kiểm thử kết nối và tính nhất quán dữ liệu mà không làm thay đổi trạng thái của database phát triển.
