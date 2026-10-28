# Troubleshooting

| Triệu chứng | Nguyên nhân | Cách kiểm tra | Cách khắc phục |
|---|---|---|---|
| `OPENAI_API_KEY=MISSING` | Key không có trong JVM | PowerShell kiểm tra biến không in giá trị | Set key cho đúng session, dùng `docker run -e OPENAI_API_KEY` |
| Docker có key, Codex thiếu | Process environment độc lập | Chạy Maven Docker trực tiếp | Không dán key vào Codex; dùng `-e` |
| Connection refused | Sai host/port/network | `docker network inspect` | Maven Docker dùng hostname container:5432 |
| `localhost:6666` fail | localhost là Maven container | Kiểm tra JDBC URL | Dùng `postgres_openai_it:5432` |
| Missing `vector` extension | Chưa chạy migration 001 | `SELECT * FROM pg_extension` | Chạy migrations trên staging riêng |
| Dimensions mismatch | Model/schema/identity khác nhau | `vector_dims`, metadata table | Controlled re-index staging; không trộn collections |
| Identity mismatch | Provider/model/version thay đổi | `vector_collection_metadata` | Không bypass guard; re-index có kiểm soát |
| Không có result | Threshold cao hoặc metadata filter không khớp | Log request/metadata | Dùng default 0.35, kiểm tra normalized locations |
| Sai chiều tuyến | Thiếu metadata hoặc query mơ hồ | Xem `departure/arrivalLocationNormalized` | Re-index sau mapper update; nhiều chặng để semantic-only |
| Vector trùng | Bỏ qua manifest/stable id | So sánh manifest và vector count | Gọi ingestion service, không insert tay |
| Maven BUILD FAILURE | `-D` bị PowerShell tách | Xem Maven phase lạ | Quote từng system property |
| Test SKIPPED | Opt-in flag vắng | Surefire report | Bật đúng flag chỉ ở staging |
| `contextLoads` fail | Docker Maven dùng localhost DB | Stack trace JDBC | Join network, override datasource acceptance |

