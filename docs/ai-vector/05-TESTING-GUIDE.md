# Testing Guide — Kiểm thử Hệ thống

## 1. Kiểm thử Toàn vẹn Dữ liệu Hợp nhất (Unified Database Data Verification)

Để xác minh PostgreSQL pgvector và tính nhất quán dữ liệu giữa Website và Chatbot:

```powershell
.\mvnw.cmd test "-Dtest=UnifiedDatabaseDataVerificationTest"
```

Kết quả kiểm tra:
- Kết nối tới `busbooking` thành công.
- Extension `vector` (version 0.8.7) hoạt động.
- Các bảng `vector_store`, `vector_collection_metadata`, `vector_ingestion_manifest` tồn tại.
- Dữ liệu nghiệp vụ thật: routes (83), trips (50,647), bookings (13,298), tickets (26,339), payments (10,921).

---

## 2. Kiểm thử AI Ticket Cancellation Assistant

```powershell
.\mvnw.cmd test "-Dtest=TicketCancellationTest"
```
Bao gồm 18 kịch bản (hủy toàn bộ, hủy một phần, giữ lại vé, chống prompt injection, chống double-click idempotency, kiểm tra giải phóng ghế, demo refund).

---

## 3. Kiểm thử Demo Payment

```powershell
.\mvnw.cmd test "-Dtest=DemoPaymentTest"
```
Bao gồm 18 kịch bản thanh toán QR nội bộ mô phỏng an toàn.

---

## 4. Chạy Kết hợp Tất cả các Bộ Kiểm thử Đã Nâng cấp

```powershell
.\mvnw.cmd test "-Dtest=UnifiedDatabaseDataVerificationTest,TicketCancellationTest,DemoPaymentTest"
```

---

## 5. Lưu ý An toàn với Acceptance Test Cũ

Test `PgVectorOpenAiStagingAcceptanceTest` được gắn assertion bắt buộc yêu cầu đúng JDBC URL `jdbc:postgresql://postgres_openai_it:5432/busbooking_openai_it`. Test này **không thể** và **không bao giờ** chạy nhầm vào database chính `busbooking`, bảo đảm an toàn dữ liệu 100%.
