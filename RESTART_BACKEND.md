# 🔄 RESTART BACKEND - BẮT BUỘC

## ⚠️ VẤN ĐỀ
- Code đã có `HELP_REQUEST` alert type
- Code đã parse `helpRequest: 1` từ MQTT
- Code đã tạo method `createHelpRequestAlert()`
- **NHƯNG** backend đang chạy vẫn dùng code CŨ (chưa có logic này)

## ✅ GIẢI PHÁP

### Bước 1: Dừng backend hiện tại
```powershell
# Trong terminal đang chạy mvn spring-boot:run
# Nhấn Ctrl+C
```

### Bước 2: Build lại project (optional nhưng nên làm)
```powershell
mvn clean compile
```

### Bước 3: Restart backend
```powershell
mvn spring-boot:run
```

### Bước 4: Đợi backend khởi động xong
Xem log cho đến khi thấy:
```
Started BfeForAiotApplication in X.XXX seconds
```

### Bước 5: Test HELP_REQUEST
```powershell
# Terminal mới
.\test-help-request.ps1
```

## 🔍 KIỂM TRA LOG

Sau khi gửi MQTT message, backend phải in ra:

```
🔍 Safety Check - MAC: F4DD40BA2010, fallDetected: 1, helpRequest: 1
⚡ Alert Check - fallDetected=1, helpRequest=1
🚨 FALL DETECTED - Creating alert...
💾 FALL alert saved to database - ID: 75
🆘 HELP REQUEST - Creating alert...
🆘 createHelpRequestAlert() called for MAC: F4DD40BA2010
✅ Creating HELP_REQUEST alert...
💾 HELP_REQUEST alert saved to database - ID: 76
📡 HELP_REQUEST alert published via WebSocket
```

## 📊 KIỂM TRA DATABASE

```sql
SELECT id, alert_type, message, severity, status, triggered_at 
FROM alerts 
WHERE alert_type IN ('FALL', 'HELP_REQUEST')
ORDER BY id DESC 
LIMIT 10;
```

Kết quả mong đợi:
- ID 75: `FALL` - 🚨 PHÁT HIỆN NGÃ
- ID 76: `HELP_REQUEST` - 🆘 YÊU CẦU TRỢ GIÚP

## ❌ NẾU VẪN KHÔNG THẤY HELP_REQUEST

1. **Kiểm tra AlertType.java có HELP_REQUEST không:**
   ```bash
   cat src/main/java/com/hatrustsoft/bfe_foraiot/model/AlertType.java
   ```
   Phải có dòng: `HELP_REQUEST,      // Yêu cầu trợ giúp (SOS)`

2. **Kiểm tra MqttMessageHandler.java:**
   ```bash
   grep -n "createHelpRequestAlert" src/main/java/com/hatrustsoft/bfe_foraiot/service/MqttMessageHandler.java
   ```
   Phải có method này và được gọi khi `helpRequest == 1`

3. **Xem full log backend** để tìm lỗi

## 🎯 TÓM TẮT
**RESTART BACKEND LÀ BẮT BUỘC!** Code Java không hot-reload như JavaScript.
