# Database và Ingestion trong Kiến trúc Hợp nhất

## 1. Migrations trên Database Chung (`busbooking`)

| File Migration | Nội dung thực thi | Trạng thái trên `busbooking` |
|---|---|:---:|
| `001-enable-pgvector.sql` | `CREATE EXTENSION IF NOT EXISTS vector;` | **Đã áp dụng (v0.8.7)** |
| `002-create-vector-store.sql` | `vector_store`, HNSW index, extension `hstore`, `uuid-ossp` | **Đã áp dụng** |
| `003-create-vector-ingestion-manifest.sql` | Bảng `vector_ingestion_manifest` lưu source/version/chunk count | **Đã áp dụng** |
| `004-create-vector-collection-metadata.sql` | Bảng `vector_collection_metadata` bảo vệ identity mô hình | **Đã áp dụng** |

`vector_store` chứa `id`, `content`, JSON `metadata`, `embedding vector(1536)`. Manifest dùng khóa chính `(source_type, source_id)`. Identity bảo vệ provider (`openai`), model (`text-embedding-3-small`), dimensions (`1536`) và configuration version (`v1`).

---

## 2. Metadata Thực tế từ Dữ liệu Nghiệp vụ Thật

Khi nạp dữ liệu từ các bảng `routes`, `trips`, `vehicles` trong `busbooking`:
- `Route`: `sourceType="route"`, `sourceId`, `routeId`, `departureLocationNormalized`, `arrivalLocationNormalized`.
- `Trip`: `sourceType="trip"`, `tripId`, `routeId`, `departureTime`, `price`.
- `Vehicle`: `sourceType="vehicle"`, `vehicleId`, `seatCapacity`, `vehicleType`.

Metadata filter luôn đảm bảo lọc đúng chiều đi/đến trước khi xét độ tương đồng vector (`cosine distance`).

---

## 3. Idempotency và Cơ chế Bảo vệ Identity (Collection Guard)

`JdbcVectorCollectionGuard` đảm bảo:
1. Không cho phép đọc/ghi nếu collection chưa được đăng ký định danh hoặc định danh bị lệch model/dimensions.
2. Tuyệt đối **không copy fake embeddings** sang database nghiệp vụ chính.
3. Không thực hiện reset `vector_store` trên database dùng chung khi chưa có kế hoạch re-index được duyệt.
