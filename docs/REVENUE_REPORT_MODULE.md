# Báo cáo doanh thu: payment + booking

## Phạm vi thay đổi

- Backend chỉ thêm `report/revenue/`, test mới và tài liệu này. Không sửa entity, repository/service/controller cũ, schema, dependency hoặc cấu hình.
- Frontend chỉ thêm `src/features/revenue-report/`. Ngoại lệ đã được cho phép: thêm import và route `reports` vào `src/App.tsx`.
- Dùng lại menu `/admin/reports`, tiêu đề/layout quản trị, `AdminPageLayout`, `DataTable`, Axios và auth store hiện có; không sửa các thành phần này.
- Trang và API yêu cầu `ROLE_ADMIN` **hoặc** `VIEW_STATISTICS`, đồng nhất với menu cũ.
- Không sửa API cũ `/api/admin/statistics/revenue`; endpoint đó vẫn giữ hành vi cũ.

## Quy tắc tính

1. Khoản thu ghi nhận là `SUM(payments.amount)` của các payment `SUCCESS` (chuẩn hóa khoảng trắng/chữ hoa).
2. Thời gian lọc là `payments.created_at`, không phải ngày đặt vé hoặc ngày khởi hành.
3. Khoảng ngày gồm cả `from` và `to`, triển khai thành `[from 00:00, (to + 1 ngày) 00:00)`.
4. `createdAt` hiện là `LocalDateTime`/timestamp không kèm timezone. Báo cáo quy ước dữ liệu đang lưu là giờ Việt Nam `Asia/Ho_Chi_Minh`, không tự suy luận/chuyển timestamp cũ từ UTC.
5. Payment nối booking → trip → route; không JOIN tickets để tránh nhân số tiền theo số ghế.
6. Số giao dịch dùng `COUNT(*)`, số booking dùng `COUNT(DISTINCT booking.id)`. Không khử các payment khác nhau chỉ vì cùng booking; không cộng số booking của các nhóm để suy ra số booking duy nhất toàn báo cáo.
7. Không cộng `Booking.totalPrice`: payment khi đổi chuyến có thể chỉ ghi nhận phụ phí, nhỏ hơn tổng giá trị booking mới.
8. Khoản thu của booking **hiện** đã hủy vẫn trong tổng tiền và được tách riêng. Trạng thái này là trạng thái booking hiện tại, không phải lịch sử trạng thái tại thời điểm thanh toán.
9. Không có giao dịch hoàn tiền trong model hiện tại: `refundDataAvailable=false`. Không suy luận số tiền hoàn bằng số booking hủy và không giả lập doanh thu ròng.
10. Đây là báo cáo theo dữ liệu payment được ghi nhận, không thay thế đối soát ngân hàng. Nếu DB đang có dữ liệu seeder, báo cáo sẽ gồm các payment mẫu đó; module không sinh hoặc tự ý loại chúng.

Backend dùng Java 17 class/record, `BigDecimal` và truy vấn tổng hợp PostgreSQL. Các query một báo cáo chạy trong transaction chỉ đọc, `REPEATABLE_READ`.
Mọi trường tiền trả về JSON **decimal string** để tránh mất chính xác khi JavaScript chuyển số lớn sang `Number`.
Frontend định dạng tiền bằng `BigInt` và phần thập phân; chỉ biểu đồ dùng số gần đúng để vẽ độ cao cột.

## API

```http
GET /api/admin/reports/revenue?from=2026-11-01&to=2026-11-30&groupBy=DAY
GET /api/admin/reports/revenue/options
```

- `from`, `to`: `yyyy-MM-dd`. Cung cấp cả hai hoặc bỏ cả hai để lấy tháng hiện tại theo giờ Việt Nam.
- `groupBy`: `DAY` (mặc định), `MONTH`, `YEAR`; không phân biệt chữ hoa/thường.
- `routeId`: tùy chọn, ID tuyến tồn tại.
- `paymentMethod`: tùy chọn, chuẩn hóa trim/uppercase; được truyền bằng bind parameter, không ghép vào SQL.
- Giới hạn `DAY`: 366 ngày; `MONTH`/`YEAR`: 3660 ngày. Các kỳ không phát sinh được điền 0.
- Options gồm tuyến đường lịch sử và các phương thức từng có payment thành công. Không loại tuyến ngừng hoạt động vì vẫn có thể cần báo cáo lịch sử.

Response giữ dạng `{ success, message, data }`. `data` gồm:

```text
filter, currency, timezone, timeBasis, generatedAt, refundDataAvailable
summary: amount, transactionCount, bookingCount,
         cancelledBookingAmount, cancelledTransactionCount, cancelledBookingCount
timeSeries[]: period, periodStart, amount, transactionCount, bookingCount
byPaymentMethod[]: paymentMethod, amount, transactionCount, bookingCount
byRoute[]: routeId, departureLocation, arrivalLocation, amount, transactionCount, bookingCount
notes[]
```

Ví dụ `amount: "1130001.00"`. HTTP 400 cho bộ lọc sai, 403 cho thiếu quyền, 503 khi không truy vấn được dữ liệu; không trả SQL hoặc chi tiết lỗi database ra client.

## Frontend

Mở **Báo Cáo Thống Kê** trong menu quản trị, hoặc `/admin/reports`.
Trang có bộ lọc ngày/cách nhóm/tuyến/thanh toán, khoảng nhanh tháng này/tháng trước/năm nay, số tổng hợp, biểu đồ SVG và ba bảng chi tiết.
Thay đổi bộ lọc chưa áp dụng không đổi nhãn báo cáo đang xem. Nhấn **Xem báo cáo** để tải dữ liệu mới.
Lỗi API được hiển thị rõ và không thay bằng số liệu giả hoặc số 0; yêu cầu cũ bị hủy khi thay báo cáo.

## Kiểm thử không đụng database làm việc

Unit/controller/security tests (không khởi động ứng dụng, không chạy seeders):

```powershell
.\mvnw.cmd "-Dtest=RevenueReportServiceTest,RevenueReportControllerTest,RevenueReportSecurityTest" test
```

Integration test opt-in dùng PostgreSQL 16 **riêng biệt**. Test có thao tác tạo/xóa fixture nên chỉ chấp nhận URL loopback với tên database chính xác `busbooking_revenue_it`; tuyệt đối không trỏ vào `busbooking`.

```powershell
docker run --rm -d --name revenue-report-it -e POSTGRES_DB=busbooking_revenue_it -e POSTGRES_USER=revenue_test -e POSTGRES_PASSWORD=revenue_test_only -p 127.0.0.1:15632:5432 postgres:16-alpine
.\mvnw.cmd "-Dtest=RevenueReport*Test" "-Drevenue.it.enabled=true" "-Drevenue.it.jdbc-url=jdbc:postgresql://127.0.0.1:15632/busbooking_revenue_it" "-Drevenue.it.username=revenue_test" "-Drevenue.it.password=revenue_test_only" test
docker stop revenue-report-it
```

Integration test kiểm tra SUM thật tại PostgreSQL: SUCCESS/FAILED/PENDING, biên timestamp microsecond, booking hủy, phụ phí đổi chuyến, nhiều payment trên cùng booking, lọc tuyến/phương thức, SQL injection, số lớn và tập dữ liệu rỗng.

Trong repository frontend:

```powershell
npm run build
.\node_modules\.bin\oxlint.cmd src/features/revenue-report
node --experimental-strip-types --test src/features/revenue-report/tests/utils.test.mjs
```

Khi Vite đang chạy, smoke test dùng `puppeteer-core` đã có cùng Chrome/Edge cài sẵn:

```powershell
node src/features/revenue-report/tests/browser-smoke.mjs http://localhost:5173
```

Mọi request `/api/` của smoke test bị chặn và giả lập; không gửi thao tác ghi nào vào backend thật.
Sau khi triển khai, cần khởi động lại backend bằng cấu hình hiện tại để nạp controller mới. Các seeder/code cũ vẫn giữ nguyên.
