# HƯỚNG DẪN SETUP FACEBOOK MESSENGER WEBHOOK

## 📋 Tổng quan
Hệ thống này tích hợp Facebook Messenger để gửi thông báo nguy hiểm real-time đến quản lý/nhân viên.

---

## 🚀 BƯỚC 1: Tạo Facebook App

1. Truy cập: https://developers.facebook.com/apps/
2. Click **"Create App"** (Tạo ứng dụng)
3. Chọn loại: **"Business"** hoặc **"Other"**
4. Điền thông tin:
   - **App Name**: BFE IoT Alert System
   - **App Contact Email**: your-email@example.com
5. Click **"Create App"**

---

## 🔧 BƯỚC 2: Thêm Messenger Product

1. Trong Dashboard app, tìm mục **"Add Products"**
2. Tìm **"Messenger"** → Click **"Set Up"**
3. Scroll xuống phần **"Access Tokens"**

---

## 🔑 BƯỚC 3: Tạo Facebook Page (nếu chưa có)

1. Truy cập: https://www.facebook.com/pages/create/
2. Tạo Page với tên: **"BFE Smart Helmet Alert"**
3. Chọn category: **"Product/Service"**
4. Điền thông tin và **Create Page**

---

## 🎫 BƯỚC 4: Generate Page Access Token

1. Quay lại **Facebook App Dashboard** → **Messenger** → **Settings**
2. Tìm mục **"Access Tokens"**
3. Click **"Add or Remove Pages"**
4. Chọn Page bạn vừa tạo → Cấp quyền:
   - ✅ `pages_messaging`
   - ✅ `pages_manage_metadata`
   - ✅ `pages_read_engagement`
5. Click **"Generate Token"**
6. **Copy token** này (dạng: `EAAxxxxxxxxxxxx`)

---

## ⚙️ BƯỚC 5: Cấu hình trong application.properties

Mở file `src/main/resources/application.properties` và cập nhật:

```properties
# Facebook Messenger Configuration
facebook.messenger.page-access-token=EAAxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
facebook.messenger.verify-token=BFE_SECURE_TOKEN_123456
facebook.messenger.api-url=https://graph.facebook.com/v18.0/me/messages
```

**Lưu ý:**
- `page-access-token`: Token vừa copy ở bước 4
- `verify-token`: Tự đặt (dùng để verify webhook)

---

## 🌐 BƯỚC 6: Deploy Backend lên Heroku

1. Commit code mới:
```bash
git add .
git commit -m "Add Facebook Messenger integration"
git push heroku main
```

2. Kiểm tra app đã chạy:
```bash
heroku logs --tail
```

3. Lấy URL app: 
```
https://your-app-name.herokuapp.com
```

---

## 🔗 BƯỚC 7: Setup Webhook trên Facebook

1. Quay lại **Facebook App Dashboard** → **Messenger** → **Settings**
2. Tìm mục **"Webhooks"**
3. Click **"Add Callback URL"**
4. Điền thông tin:
   - **Callback URL**: `https://your-app-name.herokuapp.com/api/webhook`
   - **Verify Token**: `BFE_SECURE_TOKEN_123456` (giống trong application.properties)
5. Click **"Verify and Save"**

6. Sau khi verify thành công, chọn **Subscription Fields**:
   - ✅ `messages`
   - ✅ `messaging_postbacks`
   - ✅ `messaging_optins`

7. Click **"Subscribe"**

---

## ✅ BƯỚC 8: Test Webhook

### Test 1: Gửi tin nhắn từ Messenger

1. Truy cập Facebook Page của bạn
2. Click **"Send Message"**
3. Gõ: `hello`
4. Bot sẽ reply: "👋 Xin chào! Tôi là Bot quản lý mũ bảo hộ thông minh..."

### Test 2: Test endpoint thủ công

```bash
# Lấy PSID của người nhận (trong log khi gửi tin nhắn lần đầu)
curl -X POST "https://your-app-name.herokuapp.com/api/webhook/test-alert?recipientId=YOUR_PSID&employeeName=Nguyen%20Van%20A&alertType=Gas%20vuot%20nguong&location=Cong%20truong%20A"
```

---

## 📱 BƯỚC 9: Lấy PSID của người dùng

**PSID (Page-Scoped ID)** là ID duy nhất của mỗi user khi chat với Page.

### Cách 1: Xem trong log Backend
```
# Khi user gửi tin nhắn lần đầu, log sẽ hiện:
Processing message from sender: 1234567890123456
```

### Cách 2: Sử dụng lệnh trong Messenger
Gõ trong Messenger: `status`
Bot sẽ reply với PSID của bạn.

---

## 🔥 BƯỚC 10: Tích hợp với Alert System

### Khi phát hiện nguy hiểm từ IoT sensor:

```java
@Autowired
private MessengerService messengerService;

// Khi phát hiện khí gas vượt ngưỡng
public void handleGasAlert(String employeeName, String location) {
    // Broadcast tới tất cả quản lý đã đăng ký
    messengerService.broadcastDangerAlert(
        employeeName,
        "Khí độc vượt ngưỡng nguy hiểm",
        location
    );
}
```

---

## 🧪 TEST FLOW HOÀN CHỈNH

### 1. User đăng ký nhận thông báo:
```
User: subscribe
Bot: ✅ Bạn đã đăng ký nhận thông báo cảnh báo nguy hiểm!
```

### 2. Link với mã nhân viên:
```
User: link NV001
Bot: ✅ Đã liên kết với mã nhân viên: NV001
```

### 3. Khi có cảnh báo nguy hiểm:
```
Bot: 🚨 CẢNH BÁO NGUY HIỂM!

Nhân viên: Nguyễn Văn A
Loại cảnh báo: Khí độc vượt ngưỡng
Vị trí: Khu vực công trường A
Thời gian: 31/10/2025 14:30:00

Vui lòng kiểm tra ngay!

[✅ Đã xử lý] [📞 Gọi khẩn cấp] [📍 Xem vị trí]
```

---

## 🎯 COMMANDS CHO USER

| Lệnh | Chức năng |
|------|-----------|
| `hi` / `hello` | Chào mừng |
| `help` | Xem hướng dẫn |
| `subscribe` | Đăng ký nhận thông báo |
| `unsubscribe` | Hủy nhận thông báo |
| `status` | Kiểm tra trạng thái |
| `link [mã NV]` | Liên kết với mã nhân viên |

---

## 🔒 BẢO MẬT

**⚠️ Quan trọng:**
- **KHÔNG** commit Page Access Token lên GitHub
- Sử dụng Heroku Config Vars:

```bash
heroku config:set FACEBOOK_PAGE_ACCESS_TOKEN=your_token_here
heroku config:set FACEBOOK_VERIFY_TOKEN=your_verify_token
```

Sau đó sửa `application.properties`:
```properties
facebook.messenger.page-access-token=${FACEBOOK_PAGE_ACCESS_TOKEN}
facebook.messenger.verify-token=${FACEBOOK_VERIFY_TOKEN}
```

---

## 🐛 TROUBLESHOOTING

### Lỗi "Webhook verification failed"
- Kiểm tra `verify-token` trong app.properties khớp với Facebook
- Đảm bảo backend đã deploy và chạy

### Lỗi "Invalid OAuth access token"
- Page Access Token hết hạn hoặc sai
- Generate lại token mới

### Bot không reply
- Kiểm tra log backend: `heroku logs --tail`
- Xem có nhận được webhook không
- Kiểm tra Page Access Token đúng chưa

### User không nhận được broadcast alert
- Kiểm tra user đã `subscribe` chưa
- Xem trong database: `SELECT * FROM messenger_users;`

---

## 📊 DATABASE SCHEMA

```sql
CREATE TABLE messenger_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    psid VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    employee_id VARCHAR(50) UNIQUE,
    subscribed BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_interaction TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 🎓 TÀI LIỆU THAM KHẢO

- Facebook Messenger Platform: https://developers.facebook.com/docs/messenger-platform
- Send API Reference: https://developers.facebook.com/docs/messenger-platform/send-messages
- Webhook Reference: https://developers.facebook.com/docs/messenger-platform/webhooks

---

## ✅ CHECKLIST

- [ ] Đã tạo Facebook App
- [ ] Đã tạo Facebook Page
- [ ] Đã generate Page Access Token
- [ ] Đã cấu hình application.properties
- [ ] Đã deploy lên Heroku
- [ ] Đã setup Webhook trên Facebook
- [ ] Đã test gửi tin nhắn từ Messenger
- [ ] Bot reply thành công
- [ ] Đã test endpoint /test-alert
- [ ] Đã test broadcast alert

---

**🎉 Hoàn thành! Hệ thống Messenger đã sẵn sàng gửi cảnh báo nguy hiểm!**
