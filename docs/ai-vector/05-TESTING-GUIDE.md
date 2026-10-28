# Testing guide

Tất cả lệnh Maven Docker dưới đây quote từng `-D...` vì PowerShell có thể làm hỏng argument không quote.

## Unit test

```powershell
docker run --rm -v maven_repo_openai_it:/root/.m2 -v "${PWD}:/workspace" -w /workspace `
  maven:3.9.6-eclipse-temurin-17 mvn -q test
```

Nếu `Nhom01WwwApplicationTests` cần database, join network và dùng datasource acceptance thay vì localhost.

## Maven clean verify

Lệnh này chạy suite mặc định với datasource staging riêng; không truyền `OPENAI_API_KEY`, vì vậy các test opt-in tiếp tục SKIPPED.

```powershell
$itDbPassword = $env:AI_VECTOR_IT_DB_PASSWORD
if ([string]::IsNullOrWhiteSpace($itDbPassword)) { throw 'AI_VECTOR_IT_DB_PASSWORD is missing.' }
docker run --rm --network nhom01_www_backend_busbooking-network `
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres_openai_it:5432/busbooking_openai_it `
  -e SPRING_DATASOURCE_USERNAME=postgres `
  -e SPRING_DATASOURCE_PASSWORD=$itDbPassword `
  -v maven_repo_openai_it:/root/.m2 -v "${PWD}:/workspace" -w /workspace `
  maven:3.9.6-eclipse-temurin-17 mvn -q clean verify
```

## Deterministic PgVector integration

`PgVectorStoreIntegrationTest` dùng `DeterministicEmbeddingModel`, không gọi OpenAI và cleanup `vector_store`/manifest của database nó nhận. Chỉ chạy trên acceptance riêng.

```powershell
$itDbPassword = $env:AI_VECTOR_IT_DB_PASSWORD
if ([string]::IsNullOrWhiteSpace($itDbPassword)) { throw 'AI_VECTOR_IT_DB_PASSWORD is missing.' }
docker run --rm --network nhom01_www_backend_busbooking-network -v maven_repo_openai_it:/root/.m2 `
  -v "${PWD}:/workspace" -w /workspace maven:3.9.6-eclipse-temurin-17 mvn -q `
  '-Dtest=PgVectorStoreIntegrationTest' '-Dai.vector.it.enabled=true' `
  '-Dai.vector.it.jdbc-url=jdbc:postgresql://postgres_openai_it:5432/busbooking_openai_it' `
  '-Dai.vector.it.username=postgres' ("-Dai.vector.it.password={0}" -f $itDbPassword) test
```

## Fake staging guard

`PgVectorStagingGuardIntegrationTest` kiểm tra fake vectors unregistered bị chặn mà không xóa chúng. Dùng credentials staging hiện có, không ghi password vào tài liệu.

`PgVectorStagingAcceptanceTest` dùng `VietnameseLexicalEmbeddingModel` fake và reset tables mục tiêu; không chạy trên fake collection cần bảo toàn.

## Real OpenAI acceptance

Test thật: `PgVectorOpenAiStagingAcceptanceTest`. Nó reset **chỉ** exact JDBC URL `postgres_openai_it/busbooking_openai_it`, ingest 80 staging documents, kiểm tra identity/1536D/idempotency và in Top-K an toàn.

```powershell
if ([string]::IsNullOrWhiteSpace($env:OPENAI_API_KEY)) { throw 'OPENAI_API_KEY is missing.' }
$itDbPassword = $env:AI_VECTOR_IT_DB_PASSWORD
if ([string]::IsNullOrWhiteSpace($itDbPassword)) { throw 'AI_VECTOR_IT_DB_PASSWORD is missing.' }
docker run --rm --network nhom01_www_backend_busbooking-network -e OPENAI_API_KEY `
  -v maven_repo_openai_it:/root/.m2 -v "${PWD}:/workspace" -w /workspace `
  maven:3.9.6-eclipse-temurin-17 mvn -q `
  '-Dtest=PgVectorOpenAiStagingAcceptanceTest' '-Dai.vector.acceptance.enabled=true' `
  '-Dai.vector.it.jdbc-url=jdbc:postgresql://postgres_openai_it:5432/busbooking_openai_it' `
  '-Dai.vector.it.username=postgres' ("-Dai.vector.it.password={0}" -f $itDbPassword) test
```

`mvn clean verify` mặc định skip 6 opt-in tests: OpenAI acceptance, lexical staging acceptance, staging guard và ba PgVector integration cases. Đây là chủ ý để không gọi API/reset database vô tình.

## Guard, idempotency, aliases và đúng chiều

- `JdbcVectorCollectionGuardTest`: provider/model/dimensions/version identity và collection không đăng ký.
- `VectorIngestionServiceTest`: manifest, retry, chunk cũ và idempotency.
- `VietnameseLocationNormalizerTest`: Sài Gòn/TP.HCM/Hồ Chí Minh và query nhiều chặng.
- `PgVectorSearchServiceTest`: Top-K, threshold 0.35, `sourceType`, `routeId`, no-match và hai chiều route.

## SQL kiểm tra sau test

```powershell
docker exec postgres_openai_it psql -U postgres -d busbooking_openai_it -c `
  "SELECT count(*) AS vectors, min(vector_dims(embedding)) AS min_dimensions, max(vector_dims(embedding)) AS max_dimensions FROM vector_store;"
```

Maven Docker không truy cập Windows `localhost:6666`; phải dùng Docker network và hostname container. Codex Extension có environment riêng, nên key có thể missing trong extension dù PowerShell có key.

