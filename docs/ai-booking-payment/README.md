# HỆ THỐNG AI-ASSISTED BUS BOOKING & QR DEMO PAYMENT TRỰC TIẾP TRONG CHATBOT

## 1. GIỚI THIỆU TỔNG QUAN

Tính năng **AI-Assisted Bus Booking & QR Demo Payment** mở rộng chatbot thông minh của hệ thống bán vé xe khách **VEXE (Nhom01_WWW)**, cho phép hành khách:
1. **Tìm chuyến xe bằng ngôn ngữ tự nhiên** (Tiếng Việt đa ngữ cảnh, chuẩn hóa alias địa danh như "Sài Gòn" -> "TP. Hồ Chí Minh").
2. **Gợi ý & chọn ghế cạnh nhau thông minh** dựa trên sơ đồ ghế thực tế (Floor 1/Floor 2, A01-A02...) của nhà xe.
3. **Xác nhận đặt vé an toàn** với bản tóm tắt chi phí, lộ trình và chính sách giữ chỗ 5 phút có kiểm soát giao dịch chống double booking.
4. **Thanh toán mô phỏng (Demo Payment) bằng mã QR trực tiếp ngay trong chat**:
   - Hoàn toàn độc lập nội bộ, **không tích hợp và không chuyển tiền qua VNPAY, MoMo, ZaloPay hay VietQR**.
   - Có banner cảnh báo rõ ràng: `"THANH TOÁN MÔ PHỎNG — KHÔNG CHUYỂN TIỀN THẬT"`.
   - Có countdown 5 phút (300 giây) theo thời hạn giữ chỗ của Redis.
5. **Phát hành vé điện tử tức thời**:
   - Chuyển trạng thái Payment sang `SIMULATED_PAID`, Booking sang `PAID`, Ticket sang `ISSUED`.
   - Mỗi vé có mã QR điện tử riêng biệt (`TICKET:...`) dùng để kiểm soát khi lên xe, tách biệt hoàn toàn với mã QR thanh toán demo (`DEMO_PAYMENT:...`).

---

## 2. CẤU HÌNH & CHẠY DEMO

### 2.1 Cấu hình Backend (`application.yml`)
Trong môi trường phát triển / demo đồ án:
```yaml
app:
  demo-payment:
    enabled: true  # Bật cổng thanh toán mô phỏng nội bộ
```
> **Bảo vệ môi trường Production:** Khi `app.demo-payment.enabled = false`, toàn bộ endpoint `/api/payments/demo/**` sẽ trả về `403 Forbidden` để ngăn chặn gian lận vé.

### 2.2 Khởi chạy Backend
```powershell
# Chạy ứng dụng Spring Boot Backend
.\mvnw.cmd spring-boot:run
```
Ứng dụng backend khởi động tại `http://localhost:8080`.

### 2.3 Khởi chạy Frontend
```powershell
cd D:\WWW\project\Nhom01_WWW_Frontend
npm run dev
```
Ứng dụng frontend khởi động tại `http://localhost:5173`.

---

## 3. LUỒNG THAO TÁC TRÊN GIAO DIỆN CHATBOT

1. Mở icon Chatbot ở góc phải bên dưới màn hình.
2. Nhập: `"Đặt giúp tôi 2 vé đi Đà Lạt tối mai"`.
3. AI phản hồi hỏi điểm đi: *"Dạ anh/chị muốn khởi hành từ điểm nào đến Đà Lạt ạ?"*.
4. Người dùng trả lời: `"Sài Gòn"`.
5. AI hiển thị danh sách các chuyến xe phù hợp dưới dạng thẻ (Card) trực quan.
6. Người dùng nhấn nút **"Chọn chuyến này"**.
7. AI hỏi: *"Bạn có muốn chọn 2 ghế cạnh nhau không?"*.
8. Người dùng nhấn **"Có, chọn 2 ghế cạnh nhau"**.
9. AI kiểm tra sơ đồ ghế thực tế, tự động chọn cặp ghế liền kề (ví dụ: A01, A02), tính tổng tiền và hiển thị **Booking Confirmation Card**.
10. Người dùng nhấn **"Xác nhận và thanh toán"**.
11. AI tạo booking `PENDING_PAYMENT`, giữ chỗ 5 phút và hiển thị **Demo Payment QR Card** với đồng hồ đếm ngược 5 phút.
12. Người dùng nhấn nút **"Mô phỏng thanh toán thành công"**.
13. Backend cập nhật `SIMULATED_PAID`, `PAID`, phát hành vé và hiển thị **Ticket Success Card** kèm mã QR vé điện tử cho từng hành khách.

---

## 4. TÀI LIỆU CHI TIẾT
- [Kiến trúc hệ thống](ARCHITECTURE.md)
- [Kịch bản hội thoại mẫu](CHAT-FLOW.md)
- [Hợp đồng API & Dữ liệu](API-CONTRACT.md)
- [Hướng dẫn kiểm thử & 18 scenarios](TESTING-GUIDE.md)
- [Xử lý sự cố thường gặp](TROUBLESHOOTING.md)
