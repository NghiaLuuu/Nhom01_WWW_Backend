# Developer handover

## Đã hoàn thành

- Mapping Route/Trip/Vehicle, chunk/version/manifest idempotency.
- Pgvector HNSW cosine storage, collection identity guard.
- OpenAI real acceptance trên staging riêng.
- Alias Vietnamese và directional metadata filtering.
- Unit, deterministic integration, fake guard và OpenAI acceptance tests.

## Chưa triển khai

- REST endpoint cho ingestion/search.
- Scheduler/admin job gọi `VectorIngestionService.ingestAll()`.
- Tích hợp `VectorSearchService` vào `ChatbotService` RAG.
- Hybrid lexical/BM25 reranking, availability/seat/price validation từ semantic result.

## Đọc đầu tiên

1. `VectorDocumentMapper`
2. `VectorIngestionService`
3. `PgVectorSearchService` và `VietnameseLocationNormalizer`
4. `JdbcVectorCollectionGuard`
5. `PgVectorOpenAiStagingAcceptanceTest`

## Quy trình phát triển an toàn

- Thêm metadata: mapper → unit test → re-index chỉ acceptance → acceptance test.
- Thêm alias: `VietnameseLocationNormalizer` → regression test cả hai chiều.
- Đổi mapper/model/dimensions: xem identity, controlled re-index; không reuse vectors cũ.
- Consumer mới: inject `VectorSearchService`, coi result là context gợi ý, sau đó xác minh domain data.

Không được reset, xóa, re-index hoặc trộn collection trên production. Không commit key, password, backup nhạy cảm hoặc output chứa secret.

## 30 phút đầu tiên cho developer mới

- 5 phút: đọc `01-ARCHITECTURE.md` và `04-SEMANTIC-SEARCH.md`.
- 10 phút: kiểm tra Java/Docker/network và database hiện có, không reset.
- 10 phút: chạy unit tests và đọc Surefire reports.
- 5 phút: xem Top-K acceptance output và metadata filter.

Đây là lộ trình tham khảo; môi trường mới hoàn toàn có thể cần lâu hơn để cài Docker, pull images hoặc nhận quyền truy cập secrets.

## Checklist trước commit/push

- [ ] Không có `sk-...`, password hoặc backup trong diff.
- [ ] Unit/regression tests pass.
- [ ] Acceptance chỉ chạy trên `busbooking_openai_it`.
- [ ] Không thay đổi fake vectors hoặc production.
- [ ] Mapper/identity thay đổi có migration và re-index plan.
