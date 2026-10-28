# Kiến trúc AI Vector

## Thành phần đã triển khai

| Thành phần | Vai trò |
|---|---|
| Spring Boot / Spring AI | Wiring profile `ai-vector`, OpenAI model và PgVectorStore |
| `VectorDocumentMapper` | Biến Route/Trip/Vehicle thành `Document`, metadata và stable document id |
| `VectorIngestionService` | Lập chỉ mục theo batch, idempotency, xóa chunk/source lỗi thời |
| `JdbcVectorIngestionStateStore` | Lưu manifest document version/chunk count |
| `JdbcVectorCollectionGuard` | Khóa read/write khi identity embedding không khớp |
| `PgVectorSearchService` | Top-K, threshold, source/route filters, directional filters |
| `VietnameseLocationNormalizer` | Canonical hóa alias như Sài Gòn → `tp hcm` |

## Luồng ingestion

```mermaid
flowchart LR
  R[Route] --> M[VectorDocumentMapper]
  T[Trip] --> M
  V[Vehicle] --> M
  M --> D[Document + metadata + SHA-256 version]
  D --> G[VectorCollectionGuard]
  G --> E[EmbeddingModel]
  E --> P[(pgvector vector_store)]
  D --> I[(vector_ingestion_manifest)]
  G --> C[(vector_collection_metadata)]
```

`VectorIngestionService` chỉ chạy khi được gọi rõ ràng. Không có tự động ingest khi app khởi động.

## Luồng retrieval

```mermaid
flowchart LR
  Q[User query] --> N[VietnameseLocationNormalizer]
  N --> P{Có đúng một cặp từ...đến/đi?}
  P -- Có --> F[Departure/arrival metadata filter]
  P -- Không hoặc nhiều chặng --> S[Semantic-only retrieval]
  F --> E[OpenAI query embedding]
  S --> E
  E --> V[pgvector cosine similarity]
  V --> K[Top-K VectorSearchResult]
```

Directional filter không suy luận trạng thái chuyến, ghế trống hoặc giá vé. Nó chỉ chặn kết quả đảo chiều khi query có một cặp điểm đi/đến không mơ hồ.

## Tích hợp RAG tương lai

```mermaid
flowchart LR
  U[User] --> C[Chatbot]
  C --> VS[VectorSearchService]
  VS --> X[Top-K context]
  X --> L[LLM]
  L --> A[Answer]
```

Đây chỉ là hướng tích hợp. `ChatbotService` hiện chưa gọi `VectorSearchService` — **CHƯA TRIỂN KHAI**.

