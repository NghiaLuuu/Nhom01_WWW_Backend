# Database và ingestion

## Migrations

| File | Nội dung |
|---|---|
| `001-enable-pgvector.sql` | `CREATE EXTENSION vector` |
| `002-create-vector-store.sql` | extensions hỗ trợ, `vector_store`, HNSW cosine index |
| `003-create-vector-ingestion-manifest.sql` | manifest source/version/chunk count |
| `004-create-vector-collection-metadata.sql` | collection embedding identity |

`vector_store` chứa `id`, `content`, JSON `metadata`, `embedding vector(1536)`. Manifest dùng khóa `(source_type, source_id)`. Identity bảo vệ provider/model/dimensions/configuration version.

## Metadata thực tế

Route/Trip có `sourceType`, `sourceId`, `documentVersion`, `routeId`, `departureLocationNormalized`, `arrivalLocationNormalized`. Trip có thêm `tripId`, `vehicleId`; Vehicle có `vehicleId`. Các normalized fields giúp filter đúng chiều.

## Idempotency và re-index

Mapper tạo stable UUID từ source type/id/chunk index và SHA-256 document version. Nếu version/chunk count không đổi, ingestion bỏ qua. Nếu thay mapper content, aliases, chunking, model hoặc dimensions, document version/identity có thể thay đổi.

Quy trình an toàn:

1. Chạy trên `busbooking_openai_it` riêng.
2. Xác minh collection identity và số vectors trước khi reset.
3. Reset/re-index chỉ qua controlled acceptance/admin workflow đã phê duyệt.
4. Xác minh 1536 dimensions, manifest và identity sau ingestion.

Không trộn fake/deterministic vectors với OpenAI vectors: cùng dimensions không có nghĩa cùng vector space. Guard chặn collection có vectors chưa đăng ký hoặc identity không khớp.

```sql
SELECT vector_dims(embedding), count(*)
FROM vector_store
GROUP BY vector_dims(embedding);

SELECT collection_name, embedding_provider, embedding_model,
       embedding_dimensions, configuration_version
FROM vector_collection_metadata;
```

