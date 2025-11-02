# 🚨 FACEBOOK MESSENGER INTEGRATION - TÓM TẮT

## ✅ ĐÃ HOÀN THÀNH

### 1. **Dependencies** (pom.xml)
- ✅ Thêm `spring-boot-starter-webflux` cho HTTP client

### 2. **Configuration** (application.properties)
```properties
facebook.messenger.page-access-token=YOUR_PAGE_ACCESS_TOKEN
facebook.messenger.verify-token=YOUR_VERIFY_TOKEN_123456
facebook.messenger.api-url=https://graph.facebook.com/v18.0/me/messages
```

### 3. **Entities**
- ✅ `MessengerUser.java` - Lưu thông tin người dùng Messenger

### 4. **DTOs**
- ✅ `MessengerMessageDTO.java` - DTO cho Send API
- ✅ `MessengerWebhookDTO.java` - DTO cho Webhook callback

### 5. **Repositories**
- ✅ `MessengerUserRepository.java` - JPA repository

### 6. **Services**
- ✅ `MessengerService.java` - Service gửi tin nhắn:
  - `sendTextMessage()` - Gửi tin nhắn text
  - `sendDangerAlert()` - Gửi cảnh báo nguy hiểm với Quick Replies
  - `sendButtonMessage()` - Gửi tin nhắn với buttons
  - `broadcastDangerAlert()` - Broadcast tới tất cả users đã đăng ký

### 7. **Controllers**
- ✅ `MessengerWebhookController.java` - Xử lý webhook:
  - `GET /api/webhook` - Verify webhook
  - `POST /api/webhook` - Nhận events từ Messenger
  - `POST /api/webhook/test-alert` - Test gửi alert
  - `POST /api/webhook/broadcast-alert` - Broadcast alert

---

## 📋 ENDPOINTS

### 1. Webhook Verification (Facebook gọi)
```
GET /api/webhook?hub.mode=subscribe&hub.challenge=123&hub.verify_token=YOUR_TOKEN
```

### 2. Nhận Webhook Events (Facebook gọi)
```
POST /api/webhook
Body: {Messenger webhook payload}
```

### 3. Test gửi alert thủ công
```bash
POST /api/webhook/test-alert?recipientId=USER_PSID&employeeName=Nguyen%20Van%20A&alertType=Gas&location=Area%20A
```

### 4. Broadcast alert tới tất cả
```bash
POST /api/webhook/broadcast-alert?employeeName=Nguyen%20Van%20A&alertType=Gas&location=Area%20A
```

---

## 🎯 FLOW SỬ DỤNG

### A. Setup ban đầu (1 lần)
1. Tạo Facebook App & Page (xem `MESSENGER_SETUP_GUIDE.md`)
2. Generate Page Access Token
3. Cập nhật `application.properties`
4. Deploy backend lên Heroku
5. Setup Webhook URL trong Facebook

### B. User đăng ký nhận thông báo
1. User chat với Page: `subscribe`
2. Bot reply: "✅ Bạn đã đăng ký nhận thông báo!"
3. User được lưu vào database với `subscribed=true`

### C. Khi phát hiện nguy hiểm (IoT)
```java
@Autowired
private MessengerService messengerService;

// Khi ESP32 phát hiện khí gas vượt ngưỡng
public void handleDangerAlert(String employeeName, String location) {
    messengerService.broadcastDangerAlert(
        employeeName,
        "Khí độc vượt ngưỡng nguy hiểm",
        location
    );
}
```

### D. Quản lý nhận thông báo real-time
```
🚨 CẢNH BÁO NGUY HIỂM!

Nhân viên: Nguyễn Văn A
Loại cảnh báo: Khí độc vượt ngưỡng
Vị trí: Công trường A
Thời gian: 02/11/2025 22:45:30

Vui lòng kiểm tra ngay!

[✅ Đã xử lý] [📞 Gọi khẩn cấp] [📍 Xem vị trí]
```

---

## 🔧 TÍCH HỢP VỚI MQTT

### Trong MqttMessageHandler:
```java
@Autowired
private MessengerService messengerService;

@Override
public void handleMessage(Message<?> message) {
    // Parse MQTT message
    Map<String, Object> data = parseMessage(message);
    
    // Kiểm tra nguy hiểm
    if (isGasLevelDangerous(data)) {
        // Broadcast alert qua Messenger
        messengerService.broadcastDangerAlert(
            data.get("employeeName").toString(),
            "Khí độc vượt ngưỡng nguy hiểm",
            data.get("location").toString()
        );
    }
}
```

---

## 🗄️ DATABASE

### Table: messenger_users
```sql
CREATE TABLE messenger_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    psid VARCHAR(255) NOT NULL UNIQUE,      -- Page-Scoped ID
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    employee_id VARCHAR(50) UNIQUE,         -- Link với Worker
    subscribed BOOLEAN DEFAULT TRUE,         -- Có nhận alert không
    created_at TIMESTAMP,
    last_interaction TIMESTAMP
);
```

### Query hữu ích:
```sql
-- Xem tất cả users đã đăng ký
SELECT * FROM messenger_users WHERE subscribed = TRUE;

-- Link user với employee
UPDATE messenger_users SET employee_id = 'NV001' WHERE psid = 'user_psid';

-- Unsubscribe user
UPDATE messenger_users SET subscribed = FALSE WHERE psid = 'user_psid';
```

---

## 🧪 TESTING

### 1. Test Webhook Verification
```bash
curl "http://localhost:8080/api/webhook?hub.mode=subscribe&hub.challenge=test123&hub.verify_token=BFE_SECURE_TOKEN_123456"

# Expected: test123
```

### 2. Test gửi tin nhắn
```bash
# Lấy PSID của bạn bằng cách chat với bot: "status"
# Sau đó:

curl -X POST "http://localhost:8080/api/webhook/test-alert?recipientId=YOUR_PSID&employeeName=Test%20User&alertType=Gas%20Alert&location=Test%20Area"
```

### 3. Test trong Messenger
```
You: hi
Bot: 👋 Xin chào! Tôi là Bot quản lý mũ bảo hộ thông minh...

You: help
Bot: 📋 Các lệnh có sẵn:
     • 'subscribe' - Đăng ký nhận thông báo
     ...

You: subscribe
Bot: ✅ Bạn đã đăng ký nhận thông báo cảnh báo nguy hiểm!

You: link NV001
Bot: ✅ Đã liên kết với mã nhân viên: NV001

You: status
Bot: 📊 Trạng thái của bạn:
     ✅ Đã đăng ký nhận thông báo
     🆔 Messenger ID: 1234567890
```

---

## 📱 USER COMMANDS

| Command | Function |
|---------|----------|
| `hi` / `hello` / `chào` | Chào mừng |
| `help` / `trợ giúp` | Xem hướng dẫn |
| `subscribe` / `đăng ký` | Đăng ký nhận thông báo |
| `unsubscribe` / `hủy` | Hủy nhận thông báo |
| `status` / `trạng thái` | Kiểm tra trạng thái |
| `link [mã NV]` | Link với mã nhân viên |

---

## 🚀 NEXT STEPS

1. ✅ **Deploy lên Heroku:**
```bash
git add .
git commit -m "Add Facebook Messenger integration"
git push heroku main
```

2. ✅ **Cấu hình Webhook trong Facebook:**
   - Webhook URL: `https://your-app.herokuapp.com/api/webhook`
   - Verify Token: `BFE_SECURE_TOKEN_123456`

3. ✅ **Test với Facebook Page:**
   - Gửi tin nhắn: `hi`
   - Đăng ký: `subscribe`
   - Test alert: Gọi endpoint `/test-alert`

4. ✅ **Tích hợp với MQTT:**
   - Trong MqttMessageHandler
   - Auto gửi alert khi phát hiện nguy hiểm

---

## 📚 TÀI LIỆU

- 📖 Chi tiết setup: `MESSENGER_SETUP_GUIDE.md`
- 🔗 Facebook Docs: https://developers.facebook.com/docs/messenger-platform
- 🔗 Send API: https://developers.facebook.com/docs/messenger-platform/send-messages

---

## 🎉 HOÀN THÀNH!

Hệ thống Facebook Messenger đã sẵn sàng gửi cảnh báo nguy hiểm real-time!

**Tác giả:** GitHub Copilot  
**Ngày:** 02/11/2025
