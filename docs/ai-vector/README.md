# AI Vector — Quick Start

Module `aivector` lập chỉ mục ngữ nghĩa cho `Route`, `Trip`, `Vehicle` bằng OpenAI embedding và PostgreSQL pgvector. Mục tiêu là tìm ngữ nghĩa có kiểm soát hướng tuyến, không thay thế logic đặt vé, trạng thái chuyến hay kiểm tra ghế.

## Mục lục

- [Kiến trúc](01-ARCHITECTURE.md)
- [Cấu hình](02-CONFIGURATION.md)
- [Database và ingestion](03-DATABASE-AND-INGESTION.md)
- [Semantic Search](04-SEMANTIC-SEARCH.md)
- [Testing](05-TESTING-GUIDE.md)
- [Troubleshooting](06-TROUBLESHOOTING.md)
- [Bàn giao developer](07-DEVELOPER-HANDOVER.md)

## 1. Kiểm tra công cụ

Mục đích: xác nhận Java 17, Docker và Maven Docker trước khi dùng module.

```powershell
java -version
docker version
docker run --rm maven:3.9.6-eclipse-temurin-17 mvn -version
```

Kết quả mong đợi: Java 17 và Docker daemon hoạt động. Nếu Maven trên Windows không có trong `PATH`, dùng Maven Docker như các lệnh trong tài liệu này.

Nếu chưa có source, clone repository được cấp quyền rồi vào đúng nhánh:

```powershell
git clone <repository-url> Nhom01_WWW_Backend
Set-Location .\Nhom01_WWW_Backend
git switch feature/spring-ai-pgvector
```

## 2. Chọn môi trường đúng

| Môi trường | Mục đích | Có được reset? |
|---|---|---|
| `busbooking_it` | Fake/deterministic vectors | Không; giữ nguyên 80 vector mẫu |
| `busbooking_openai_it` trong `postgres_openai_it` | OpenAI acceptance riêng | Có, chỉ bằng harness acceptance |
| Production | Dữ liệu thật | Không |

Không dùng `localhost:6666/busbooking` từ Maven Docker. Xem [Configuration](02-CONFIGURATION.md).

## 3. Môi trường mới hoàn toàn: PostgreSQL acceptance riêng

Mục đích: tạo một container pgvector độc lập, không sửa PostgreSQL local hay production. Lệnh này **chỉ nên dùng khi container/volume chưa tồn tại**.

```powershell
$network = 'nhom01_www_backend_busbooking-network'
if ([string]::IsNullOrWhiteSpace($env:POSTGRES_PASSWORD)) { throw 'POSTGRES_PASSWORD is missing.' }
docker network inspect $network
docker volume create busbooking_openai_it_data
docker run -d --name postgres_openai_it --network $network `
  -e POSTGRES_DB=busbooking_openai_it `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD `
  -v busbooking_openai_it_data:/var/lib/postgresql/data `
  pgvector/pgvector:0.8.7-pg16-bookworm
docker exec postgres_openai_it pg_isready -U postgres -d busbooking_openai_it
```

Kết quả mong đợi: `accepting connections`. Nếu container đã tồn tại, không xóa nó; kiểm tra bằng `docker inspect postgres_openai_it`.

## 4. Cấu hình API key an toàn

Mục đích: để Spring AI đọc key từ process, không từ source code.

```powershell
if ([string]::IsNullOrWhiteSpace($env:OPENAI_API_KEY)) {
  throw 'OPENAI_API_KEY is missing in this PowerShell session.'
}
```

Không in key. File cấu hình dùng `${OPENAI_API_KEY}`. Nếu Codex Extension báo missing nhưng PowerShell có key, chạy Maven Docker trực tiếp với `-e OPENAI_API_KEY`; xem [Troubleshooting](06-TROUBLESHOOTING.md).

## 5. Chạy migrations

Mục đích: tạo extension và ba bảng vector. Không dùng Bash stdin redirection trong PowerShell.

```powershell
Get-ChildItem .\docker\postgres\migrations\*.sql | Sort-Object Name | ForEach-Object {
  docker cp $_.FullName "postgres_openai_it:/tmp/$($_.Name)"
  docker exec postgres_openai_it psql -v ON_ERROR_STOP=1 -U postgres `
    -d busbooking_openai_it -f "/tmp/$($_.Name)"
}
```

Kết quả mong đợi: extension `vector` và `vector_store`, `vector_ingestion_manifest`, `vector_collection_metadata` tồn tại. Nếu migration lỗi, dừng và xem [Database và ingestion](03-DATABASE-AND-INGESTION.md); không thử trên production.

## 6. Chuẩn bị dữ liệu nghiệp vụ

Acceptance mẫu yêu cầu đúng 10 Route, 50 Trip, 20 Vehicle. Kiểm tra chỉ đọc:

```powershell
docker exec postgres_openai_it psql -U postgres -d busbooking_openai_it -c `
  "SELECT (SELECT count(*) FROM routes) AS routes, (SELECT count(*) FROM trips) AS trips, (SELECT count(*) FROM vehicles) AS vehicles;"
```

Môi trường mới hoàn toàn chưa có dữ liệu nghiệp vụ sẽ không chạy acceptance được. Restore một backup staging đã được phê duyệt, **không** copy `vector_store` hoặc `vector_ingestion_manifest`.

## 7. Chạy ingestion

`VectorIngestionService.ingestAll()` là entry point thật. Hiện chưa có REST endpoint, CLI command, startup runner hoặc scheduler để gọi nó — **CHƯA TRIỂN KHAI**. Một admin job hoặc integration được phê duyệt phải inject service này và gọi `ingestAll()` trong profile `ai-vector`.

Để xác minh implementation end-to-end mà không tự viết job mới, chạy OpenAI acceptance theo [Testing guide](05-TESTING-GUIDE.md). Test gọi service thật trên `busbooking_openai_it`.

## 8. Kiểm tra vectors

```powershell
docker exec postgres_openai_it psql -U postgres -d busbooking_openai_it -c `
  "SELECT (SELECT count(*) FROM vector_store) AS vectors, (SELECT count(*) FROM vector_ingestion_manifest) AS manifests, (SELECT count(*) FROM vector_collection_metadata) AS identities, (SELECT min(vector_dims(embedding)) FROM vector_store) AS min_dimensions, (SELECT max(vector_dims(embedding)) FROM vector_store) AS max_dimensions;"
```

Với dữ liệu staging mẫu sau real acceptance: 80 vectors, 80 manifests, 1 identity, dimensions 1536. Đây là số liệu mẫu, không phải ràng buộc production.

## 9. Chạy Semantic Search

`VectorSearchService.search(VectorSearchRequest)` là API Java hiện có. Không có HTTP search endpoint — **CHƯA TRIỂN KHAI**. Ví dụ consumer Java:

```java
List<VectorSearchResult> results = vectorSearchService.search(
    new VectorSearchRequest("Tôi muốn đi từ Sài Gòn đến Đà Lạt", 5, null, null, null));
```

Kết quả được đọc bằng `content`, `metadata`, `similarityScore`. Với query có hướng, metadata filter buộc `TP.HCM → Đà Lạt`; xem [Semantic Search](04-SEMANTIC-SEARCH.md).

## 10. Xác minh thành công

Chạy real acceptance. Kết quả staging mẫu đã xác minh: OpenAI `text-embedding-3-small`, 1536 dimensions, 80 documents, lần ingestion thứ hai ghi 0 document mới, và query Sài Gòn → Đà Lạt trả TP.HCM → Đà Lạt đúng hướng.

