# Semantic Search

`PgVectorSearchService` nhận `VectorSearchRequest(query, topK, similarityThreshold, sourceType, routeId)`.

- `topK`: 1–20, mặc định 5.
- Default `similarityThreshold`: 0.35. Đây là ngưỡng recall, không phải quyết định đúng/sai hướng tuyến.
- `sourceType`: `ROUTE`, `TRIP`, `VEHICLE`.
- `routeId`: filter metadata route.

## Ví dụ Sài Gòn → Đà Lạt

1. Normalizer đổi `Sài Gòn` thành `tp hcm`; `Đà Lạt` thành `da lat`.
2. Parser nhận dạng một cặp `từ ... đến ...`.
3. Search thêm metadata filter `departureLocationNormalized=tp hcm` và `arrivalLocationNormalized=da lat`.
4. Pgvector chạy cosine similarity chỉ trên candidate đúng chiều.

Trong staging mẫu, raw semantic Top-5 có thể vẫn chứa tuyến Đà Lạt → Nha Trang ở vị trí thấp. Directional filter loại candidate này. Có thể chỉ trả 4 kết quả với `topK=5` nếu chỉ có 4 Route/Trip khớp đúng metadata; service không bịa kết quả thứ năm.

Query không địa điểm giữ semantic-only retrieval. Query nhiều chặng như “TP.HCM đến Đà Lạt rồi đến Nha Trang” không bị ép thành một route pair để tránh false negative.

Semantic Search không phải nguồn sự thật về ghế trống, giá cuối cùng hoặc chuyến đang bán. Consumer phải gọi domain service/repository phù hợp để xác minh nghiệp vụ.

