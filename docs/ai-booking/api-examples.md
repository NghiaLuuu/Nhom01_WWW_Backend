# Hướng Dẫn Gọi API & Ví Dụ Thực Tế (API Examples)

## 1. Endpoint Tương Tác Với Chatbot & AI Assistant

Hệ thống cung cấp 2 endpoint HTTP POST mở rộng:
1. `POST /api/chatbot/ask`: Endpoint tổng quát, tự động nhận diện ý định đặt vé để chuyển giao cho AI Assistant, đồng thời xử lý các câu hỏi RAG và FAQ thông thường.
2. `POST /api/chatbot/booking-assistant`: Endpoint chuyên dụng cho việc hỗ trợ đặt vé, chọn ghế và xác nhận đơn hàng qua hội thoại.

---

## 2. Kịch bản hội thoại mẫu (Multi-turn Conversation)

### Lượt 1: Người dùng yêu cầu tìm và đặt vé
* **Endpoint**: `POST /api/chatbot/booking-assistant`
* **Headers**: `Content-Type: application/json`
* **Request Payload**:
```json
{
  "question": "Đặt giúp tôi 2 vé từ Sài Gòn đi Đà Lạt ngày mai lúc 21 giờ",
  "history": []
}
```

* **Response Payload (Bản tóm tắt đặt vé - Chờ xác nhận)**:
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "status": "CONFIRMATION_REQUIRED",
    "answer": "📋 **TÓM TẮT ĐƠN ĐẶT VÉ CHỜ XÁC NHẬN**\n- Tuyến: TP. Hồ Chí Minh -> Đà Lạt\n- Giờ khởi hành: 21:00 ngày 03/11/2026\n- Xe: Giường nằm\n- Ghế chọn (2 ghế): A01, A02\n- Đơn giá: 250.000 VNĐ/vé\n- **Tổng thanh toán: 500.000 VNĐ**\n\n⚠️ Quý khách vui lòng kiểm tra kỹ và **XÁC NHẬN** để hệ thống tạo đơn và giữ chỗ trong 5 phút.\n\n👉 **Anh/chị vui lòng trả lời 'Đồng ý' hoặc 'Xác nhận' để em tiến hành đặt vé nhé!** (Nếu muốn đổi ý hoặc hủy, anh/chị nhắn 'Hủy' giúp em).",
    "draft": {
      "idempotencyKey": "a90df1b8-2a74-4b52-b88e-4a6c8e312a10",
      "tripId": 1001,
      "routeName": "TP. Hồ Chí Minh -> Đà Lạt",
      "seatIds": [101, 102],
      "seatNames": ["A01", "A02"],
      "pricePerTicket": 250000,
      "totalPrice": 500000,
      "totalPriceFormatted": "500.000 VNĐ",
      "vehicleType": "Giường nằm",
      "requiresConfirmation": true
    }
  }
}
```

---

### Lượt 2: Người dùng gửi xác nhận đồng ý đặt vé
* **Endpoint**: `POST /api/chatbot/booking-assistant`
* **Headers**:
  - `Content-Type: application/json`
  - `Authorization: Bearer <JWT_ACCESS_TOKEN>` (Bắt buộc phải có token người dùng đã đăng nhập)
* **Request Payload**:
```json
{
  "question": "Tôi đồng ý",
  "history": [
    {
      "role": "user",
      "content": "Đặt giúp tôi 2 vé từ Sài Gòn đi Đà Lạt ngày mai lúc 21 giờ"
    },
    {
      "role": "assistant",
      "content": "📋 TÓM TẮT ĐƠN ĐẶT VÉ CHỜ XÁC NHẬN: Tuyến TP.HCM -> Đà Lạt, 2 ghế A01, A02. Tổng tiền: 500.000 VNĐ..."
    }
  ]
}
```

* **Response Payload (Đặt vé thành công - Chờ thanh toán)**:
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "status": "BOOKING_CREATED",
    "bookingId": 902,
    "bookingCode": "VX1730539821001",
    "totalPrice": 500000,
    "answer": "🎉 **ĐẶT VÉ THÀNH CÔNG!**\n\n- **Mã đặt vé (Booking Code)**: `VX1730539821001`\n- **Tuyến xe**: TP. Hồ Chí Minh -> Đà Lạt\n- **Khởi hành**: 21:00 ngày 03/11/2026\n- **Ghế đã đặt**: A01, A02\n- **Tổng tiền**: 500.000 VNĐ\n- **Trạng thái**: `PENDING_PAYMENT` (Chờ thanh toán)\n- **Thời hạn giữ chỗ**: **5 phút**\n\n💳 *Hướng dẫn thanh toán*: Đơn đặt vé đã tạo thành công và giữ chỗ trong 5 phút. Vui lòng thanh toán trực tuyến qua cổng VNPay để nhận vé điện tử."
  }
}
```

---

### Lượt 3: Người dùng kiểm tra trạng thái vé sau khi thanh toán
* **Endpoint**: `POST /api/chatbot/booking-assistant`
* **Request Payload**:
```json
{
  "question": "Kiểm tra giúp tôi đơn đặt vé mã 902",
  "history": []
}
```
* **Response Payload**: Trả về tình trạng hiện thời của đơn hàng (`PAID` hoặc `PENDING_PAYMENT`).
