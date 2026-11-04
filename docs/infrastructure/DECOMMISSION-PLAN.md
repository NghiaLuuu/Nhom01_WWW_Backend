# Kế hoạch Thu hồi & Dọn dẹp Tài nguyên Thừa (Decommission Plan)

> **LƯU Ý QUAN TRỌNG:** Tuân thủ nguyên tắc an toàn dữ liệu, kế hoạch này chỉ lập danh sách kiểm kê và đánh giá rủi ro. **Không có bất kỳ lệnh xóa container hoặc xóa volume nào được tự ý thực thi trong phiên làm việc này.** Mọi thao tác hủy tài nguyên chỉ được tiến hành khi có sự phê duyệt rõ ràng từ Chủ sở hữu (Repository Owner).

---

## 1. Bảng Kiểm kê Tài nguyên Ứng viên (Audit Candidate Inventory)

| STT | Thành phần (Component) | Loại tài nguyên | Container / Tên | Database | Volume gắn kết | Vai trò lịch sử | Có dữ liệu cần giữ? | Hành động đề xuất |
|:---:|:---|:---|:---|:---|:---|:---|:---:|:---|
| 1 | `postgres_db_local` | Container | `postgres_db_local` | `busbooking` | `nhom01_www_backend_postgres_data` | **Nguồn dữ liệu chính (Primary Shared DB)** | **CÓ (BẮT BUỘC BẢO TỒN)** | Đã nâng cấp lên `pgvector:0.8.7-pg16-bookworm`, áp dụng migration 001–004, giữ nguyên vẹn 100% dữ liệu. |
| 2 | `postgres_openai_it` | Container | `postgres_openai_it` | `busbooking_openai_it` | `busbooking_openai_it_data` | Container staging cũ tạo riêng cho test OpenAI acceptance | **KHÔNG** (Chỉ chứa 80 vector mẫu staging test) | **Đề xuất Decommission** sau khi phê duyệt. |
| 3 | `nhom01_ai_vector_it_pg` | Container | `nhom01_ai_vector_it_pg` | `busbooking_it` | `nhom01_ai_vector_it_pgdata` | Container test cũ dùng cho vector giả lập deterministic | **KHÔNG** (Chỉ chứa vector giả) | **Đề xuất Decommission** sau khi phê duyệt. |
| 4 | `busbooking_openai_it_data` | Volume | - | - | `busbooking_openai_it_data` | Lưu trữ dữ liệu của container `postgres_openai_it` | **KHÔNG** | **Đề xuất Decommission** sau khi phê duyệt. |
| 5 | `nhom01_ai_vector_it_pgdata` | Volume | - | - | `nhom01_ai_vector_it_pgdata` | Lưu trữ dữ liệu của container `nhom01_ai_vector_it_pg` | **KHÔNG** | **Đề xuất Decommission** sau khi phê duyệt. |
| 6 | `nhom01_ai_vector_it_net` | Network | `nhom01_ai_vector_it_net` | - | - | Bridge network tạo riêng cho 2 container test trên | **KHÔNG** | **Đề xuất Decommission** sau khi 2 container trên bị xóa. |
| 7 | `maven_repo_openai_it` | Volume | - | - | `maven_repo_openai_it` | Cache local .m2 repo cho container Maven Docker | Có thể giữ lại hoặc xóa để giải phóng dung lượng đĩa | Tùy chọn dọn dẹp. |

---

## 2. Kiểm kê Container Thuộc Dự án Khác (TUYỆT ĐỐI KHÔNG CHẠM VÀO)

Các container sau được phát hiện trên Docker Host nhưng thuộc về các dự án khác của lập trình viên, **nghiêm cấm can thiệp hoặc thay đổi**:
- `react-webfashion-*` / `webfashion-*`
- `simmes-v3-*`
- `local_timescaledb`, `local_redis`, `local_rabbitmq`, `local_mosquitto`

---

## 3. Điều kiện Tiên quyết Trước khi Thu hồi (Decommission Checklist)

- [x] Nguồn dữ liệu chính (`postgres_db_local`, database `busbooking`) đã được xác định và hoạt động ổn định.
- [x] Bản sao lưu hoàn chỉnh đã được tạo:
  - Logical SQL Dump: `backups/busbooking_backup.sql` (13.5 MB)
  - Physical Volume Tarball: `backups/nhom01_www_backend_postgres_data_backup.tar.gz` (21.5 MB)
- [x] Khả năng khôi phục đã được kiểm chứng thành công trên container thử nghiệm độc lập.
- [x] Các bảng migrations 001–004 (`vector_store`, `vector_collection_metadata`, `vector_ingestion_manifest`) đã được áp dụng an toàn trên database chính.
- [x] Bộ test xác minh kết nối và tính nhất quán dữ liệu (`UnifiedDatabaseDataVerificationTest`) chạy thành công 4/4 kịch bản.
- [x] Toàn bộ 18 test vé xe (`TicketCancellationTest`) và 18 test thanh toán demo (`DemoPaymentTest`) đã chạy thành công 36/36 kịch bản.
- [ ] Chủ sở hữu hệ thống (Repository Owner) xem xét và cấp quyền xóa danh sách tài nguyên staging.

---

## 4. Lệnh Thực thi Thu hồi An toàn (Chỉ chạy sau khi được phê duyệt)

Khi chủ sở hữu phê duyệt, lập trình viên có thể copy các lệnh sau để xóa các container và volume staging không còn sử dụng:

```powershell
# 1. Dừng và xóa 2 container staging
docker rm -f postgres_openai_it nhom01_ai_vector_it_pg

# 2. Xóa các volume staging tương ứng (KHÔNG chạm vào nhom01_www_backend_postgres_data)
docker volume rm busbooking_openai_it_data nhom01_ai_vector_it_pgdata

# 3. Xóa network staging riêng
docker network rm nhom01_ai_vector_it_net
```

---

## 5. Phương án Phục hồi & Rollback Khẩn cấp (Rollback Plan)

Trong trường hợp có bất kỳ sự cố ngoài ý muốn nào:
1. **Dữ liệu database chính:** Dễ dàng phục hồi bằng lệnh:
   ```powershell
   docker exec -i postgres_db_local psql -U postgres -d busbooking < ./backups/busbooking_backup.sql
   ```
2. **Khôi phục toàn bộ volume ban đầu:**
   ```powershell
   docker run --rm -v nhom01_www_backend_postgres_data:/to -v ${PWD}/backups:/from alpine `
     tar xzf /from/nhom01_www_backend_postgres_data_backup.tar.gz -C /to
   ```
