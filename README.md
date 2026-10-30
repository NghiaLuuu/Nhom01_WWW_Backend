# 🚌 VEXE - Online Bus Ticket Booking System (Backend)

Tài liệu API quản lý khách hàng/nhân viên, duyệt hủy vé và doanh thu: [docs/admin-management-api.md](docs/admin-management-api.md).

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.2-brightgreen.svg)
![Java](https://img.shields.io/badge/Java-17%2B-blue.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)
![Redis](https://img.shields.io/badge/Redis-Latest-red.svg)

> **Sinh viên thực hiện:** Lưu Trung Nghĩa - MSSV: 21058181  
> **Môn học:** Đăng ký đề tài WWW  
> **Mô tả:** Backend API cung cấp nền tảng xử lý nghiệp vụ cốt lõi cho Hệ thống Đặt Vé Xe Khách trực tuyến (VEXE).

---

## 🌟 Tính Năng Nổi Bật (Key Features)

* **Multi-role RBAC (Dynamic Permissions)**: Hệ thống bảo mật nâng cao sử dụng Spring Security & JWT, phân quyền động tới từng `Permission` cho các Role (`ADMIN`, `STAFF`, `CUSTOMER`).
* **Text-to-SQL AI Chatbot**: Tích hợp Spring AI (OpenAI API) cho phép khách hàng tra cứu chuyến đi, giờ giấc bằng ngôn ngữ tự nhiên, AI tự động map thành câu truy vấn SQL thông qua `TableMetadata`.
* **5-Minute Seat Hold Logic**: Xử lý logic giữ ghế an toàn trong 5 phút khi khách hàng đang ở màn hình thanh toán. Giải phóng ghế tự động nếu hết thời gian hoặc thanh toán thất bại.
* **Async Email OTP**: Tích hợp Redis và Spring Mail để xử lý xác thực OTP qua Email bất đồng bộ, mang lại trải nghiệm mượt mà không làm tắc nghẽn luồng đăng ký.

---

## 🛠️ Công Nghệ Sử Dụng (Tech Stack)

* **Core Framework:** Java 17+, Spring Boot
* **Security:** Spring Security, JWT (JSON Web Tokens)
* **Database & ORM:** PostgreSQL, Hibernate, Spring Data JPA
* **Caching & Temp Storage:** Redis (Dùng cho OTP và Hold Seat Cache)
* **AI Integration:** Spring AI, OpenAI API

---

## ⚙️ Yêu Cầu Môi Trường (Prerequisites)

Để chạy dự án, bạn cần cài đặt sẵn:
* **Java 17** hoặc mới hơn.
* **Maven** (hoặc sử dụng wrapper `mvnw` đi kèm).
* **PostgreSQL** (chạy trên port 5432).
* **Redis** (chạy trên port 6379).

---

## 🚀 Hướng Dẫn Cài Đặt (Environment Setup)

> **Lưu ý bảo mật:** File `src/main/resources/application.yml` chứa thông tin nhạy cảm (như OpenAI API Key, mật khẩu DB) nên đã được đưa vào `.gitignore` để không bị push lên GitHub. Bạn cần tự tạo file `application.yml` này tại local của mình theo mẫu sau:

Bạn cần cấu hình các thông số hệ thống trong file `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:6666/busbooking
    username: <your_db_username>
    password: <your_db_password>
  
  data:
    redis:
      host: localhost
      port: 6379

  mail:
    host: smtp.gmail.com
    port: 587
    username: <your_email>
    password: <your_app_password>

  ai:
    openai:
      api-key: <your_openai_api_key>

jwt:
  secret: <your_256_bit_jwt_secret_key>
  expiration: 86400000
```

---

## 💻 Hướng Dẫn Chạy Dự Án (Run Instructions)

**1. Tạo database trong PostgreSQL:**
```sql
CREATE DATABASE busbooking;
```

**2. Chạy ứng dụng bằng Maven:**
Mở terminal tại thư mục gốc của project (chứa file `pom.xml`):
```bash
# Đối với Windows:
.\mvnw spring-boot:run

# Đối với MacOS/Linux:
./mvnw spring-boot:run
```

Hệ thống sẽ tự động khởi động tại: `http://localhost:8080`

---

## 🧪 Tạo Dữ Liệu Mẫu (Data Seeder) & Khắc Phục Lỗi

Hệ thống cung cấp sẵn cơ chế `DataSeeder` để reset database và tự động sinh dữ liệu mẫu (Role, Permission khớp chuẩn `@PreAuthorize`, Tài khoản, Chuyến xe, v.v.).
**Lưu ý quan trọng:** Seeder đã được cấu hình **KHÔNG tự động chạy** khi khởi động Backend (để tránh vô tình xóa mất dữ liệu đang làm việc).

**Để chạy Seeder (Reset toàn bộ DB), mở terminal mới và chạy:**
```bash
.\mvnw spring-boot:run "-Dspring-boot.run.arguments=--seed"
```
*(Sau khi thấy log báo thành công, bạn nhấn `Ctrl + C` để thoát)*

**Các tài khoản test có sẵn sau khi seed:**

| Quyền hạn | Email | Mật khẩu |
| :--- | :--- | :--- |
| **Admin** | `admin@vexe.vn` | `password123` |
| **Staff** | `staff@vexe.vn` | `password123` |
| **Khách hàng** | `khach1@gmail.com` | `password123` |

> *Lưu ý: Mật khẩu đã được băm (hash) cẩn thận trong cơ sở dữ liệu.*

**Mã vé cố định để test app soát vé sau khi seed:**

| Mã vé | Trường hợp |
| :--- | :--- |
| `QR-VALID-TODAY` | Đã thanh toán, trạng thái `ISSUED` (tương đương chưa dùng), khởi hành hôm nay |
| `QR-ALREADY-USED` | Vé đã dùng, có `usedAt` |
| `QR-UNPAID` | Booking đang `PENDING_PAYMENT` |
| `QR-WRONG-DAY` | Đã thanh toán nhưng khởi hành ngày mai |

Backend lắng nghe trên `0.0.0.0:8080`. Khi test từ điện thoại, dùng IP LAN của máy chạy Backend, ví dụ `http://192.168.1.10:8080`, không dùng `localhost`.
