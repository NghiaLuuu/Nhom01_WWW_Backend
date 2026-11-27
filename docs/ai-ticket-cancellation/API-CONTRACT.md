# Tài liệu API Hợp đồng (API Contract)

Endpoint chính phục vụ cho quy trình hủy vé bằng AI Chatbot nằm tại:
`POST /api/chatbot/cancellation-assistant` hoặc kênh tổng hợp `POST /api/chatbot/chat`.

---

## 1. Request Body

### Endpoint: `POST /api/chatbot/cancellation-assistant`
Yêu cầu Header:
- `Authorization: Bearer <JWT_TOKEN>` (Bắt buộc)
- `Content-Type: application/json`

```json
{
  "message": "Hủy giúp tôi 2 vé đi Đà Lạt tối mai",
  "conversationId": "c4794270-496a-4934-8be0-b2f67eeea461",
  "action": "LOOKUP", // Các giá trị: "LOOKUP", "SELECT_BOOKING", "CONFIRM", "KEEP"
  "bookingId": 101, // Tùy chọn khi action là SELECT_BOOKING
  "confirmationToken": "f7d3a5e1-92b0-4591-a1e6-c56b71f98d40", // Bắt buộc khi action là CONFIRM
  "seatNumbers": ["A01", "A02"] // Tùy chọn nếu hủy một phần
}
```

---

## 2. Response Payloads

### 2.1. Tra cứu nhiều đơn đặt vé (BOOKING_LOOKUP)
Trường hợp tìm thấy nhiều đơn hoặc cần người dùng xác nhận đơn cụ thể:

```json
{
  "message": "Tôi tìm thấy 2 đơn đặt vé đi Đà Lạt. Vui lòng chọn đơn bạn muốn hủy:",
  "type": "BOOKING_LOOKUP",
  "candidates": [
    {
      "bookingId": 101,
      "bookingCode": "BK101",
      "tripId": 50,
      "routeName": "Sài Gòn - Đà Lạt",
      "departureTime": "2026-11-03T20:00:00",
      "status": "PAID",
      "totalPrice": 500000,
      "seatCount": 2,
      "seatNumbers": ["A01", "A02"]
    },
    {
      "bookingId": 102,
      "bookingCode": "BK102",
      "tripId": 55,
      "routeName": "Sài Gòn - Đà Lạt",
      "departureTime": "2026-11-05T08:00:00",
      "status": "PAID",
      "totalPrice": 250000,
      "seatCount": 1,
      "seatNumbers": ["B04"]
    }
  ]
}
```

### 2.2. Xem trước hủy vé và biểu phí hoàn (CANCELLATION_PREVIEW)

```json
{
  "message": "Vui lòng kiểm tra lại thông tin và xác nhận hủy vé trước khi hết hạn (5 phút):",
  "type": "CANCELLATION_PREVIEW",
  "preview": {
    "bookingId": 101,
    "bookingCode": "BK101",
    "tripId": 50,
    "routeName": "Sài Gòn - Đà Lạt",
    "departureTime": "2026-11-03T20:00:00",
    "hoursUntilDeparture": 27.5,
    "selectedSeats": ["A01", "A02"],
    "totalOriginalPrice": 500000,
    "cancellationFeePercent": 10,
    "cancellationFeeAmount": 50000,
    "refundAmount": 450000,
    "policyDescription": "Khởi hành trước hơn 24 giờ: Phí hủy 10%, hoàn trả 90% giá vé.",
    "isEligible": true,
    "confirmationToken": "f7d3a5e1-92b0-4591-a1e6-c56b71f98d40",
    "expiresAt": "2026-11-02T17:20:00Z"
  }
}
```

### 2.3. Hủy vé thành công (CANCELLATION_SUCCESS)

```json
{
  "message": "Hủy vé thành công! Số tiền hoàn 450,000 VND đã được cộng vào tài khoản ví Demo của bạn.",
  "type": "CANCELLATION_SUCCESS",
  "result": {
    "bookingId": 101,
    "bookingCode": "BK101",
    "cancelledSeats": ["A01", "A02"],
    "remainingSeats": [],
    "bookingStatus": "CANCELLED",
    "refundAmount": 450000,
    "refundStatus": "REFUNDED",
    "refundTransactionId": "DEMO-REF-9812401",
    "completedAt": "2026-11-02T17:15:30Z",
    "message": "Hủy vé và hoàn tiền mô phỏng thành công."
  }
}
```

---

## 3. Mã lỗi HTTP và Ý nghĩa (HTTP Status Codes)

| Mã lỗi | Nguyên nhân | Thông báo chi tiết |
|:---|:---|:---|
| `400 BAD_REQUEST` | Thông tin yêu cầu không hợp lệ hoặc dữ liệu vé bị xung đột. | Vé đã sử dụng hoặc mã vé không nằm trong đơn. |
| `401 UNAUTHORIZED` | Khách hàng chưa đăng nhập hoặc Token JWT hết hạn. | Vui lòng đăng nhập để quản lý đơn vé của bạn. |
| `403 FORBIDDEN` | Cố tình truy cập hoặc hủy vé thuộc quyền sở hữu của khách hàng khác. | Bạn không có quyền truy cập đơn đặt vé này. |
| `404 NOT_FOUND` | Không tìm thấy mã đơn vé hoặc chuyến xe trong hệ thống. | Không tìm thấy đơn vé tương ứng. |
| `409 CONFLICT` | Xung đột đồng thời (vé đang được cập nhật bởi tiến trình khác). | Đơn vé đang được xử lý, vui lòng thử lại sau. |
| `410 GONE` | Mã `confirmationToken` đã quá hạn 5 phút (TTL expired). | Yêu cầu xem trước hủy vé đã hết hạn. Vui lòng thao tác lại. |
