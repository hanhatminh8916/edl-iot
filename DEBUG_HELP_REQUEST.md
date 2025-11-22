# 🔍 DEBUG HELP_REQUEST KHÔNG LƯU VÀO DATABASE

## ✅ CODE ĐÃ ĐÚNG
- ✅ Parse `helpRequest` từ MQTT: Line 104
- ✅ Check `if (helpRequest == 1)`: Line 211
- ✅ Call `createHelpRequestAlert(data)`: Line 213
- ✅ Method `createHelpRequestAlert()` có: Line 485-577
- ✅ `alertRepository.save(alert)`: Line 524
- ✅ AlertType.HELP_REQUEST enum: Đã có

## ❓ TẠI SAO KHÔNG LƯU?

### Khả năng 1: MQTT message không đến backend
```bash
# Kiểm tra log Heroku
heroku logs --tail --app edl-safework-iot | grep "helpRequest"
```

Cần thấy:
```
🔍 Safety Check - MAC: F4DD40BA2010, fallDetected: X, helpRequest: 1
⚡ Alert Check - fallDetected=X, helpRequest=1
🆘 HELP REQUEST - Creating alert...
```

### Khả năng 2: Exception khi save database
```bash
# Tìm lỗi trong log
heroku logs --tail --app edl-safework-iot | grep "CRITICAL ERROR"
```

Có thể thấy:
```
❌ CRITICAL: Failed to save HELP_REQUEST alert to database
❌❌❌ CRITICAL ERROR creating help request alert for MAC F4DD40BA2010: ...
```

### Khả năng 3: Debounce đang chặn
Nếu đã tạo alert trong vòng 30 giây trước, sẽ skip:
```
⏭️ Skip duplicate help request alert (debounce: Xs since last)
```

### Khả năng 4: Database constraint violation
Alert table có thể có unique constraint mâu thuẫn

## 🔧 CÁCH TEST

### Test 1: Gửi MQTT message
```powershell
.\test-help-request.ps1
```

### Test 2: Xem log realtime
```powershell
heroku logs --tail --app edl-safework-iot
```

### Test 3: Kiểm tra database
```sql
-- Xem tất cả alerts hôm nay
SELECT id, alert_type, message, severity, status, triggered_at 
FROM alerts 
WHERE DATE(triggered_at) = CURRENT_DATE
ORDER BY id DESC;

-- Đếm số alert theo type
SELECT alert_type, COUNT(*) as count
FROM alerts
WHERE DATE(triggered_at) = CURRENT_DATE
GROUP BY alert_type;
```

### Test 4: Xóa cache debounce (nếu cần)
Restart Heroku app để clear HashMap:
```powershell
heroku restart --app edl-safework-iot
```

## 🎯 NEXT STEPS

1. **Chạy test-help-request.ps1**
2. **Xem log Heroku** để tìm lỗi
3. **Gửi log cho tôi** để debug tiếp

## 📋 LOG CẦN TÌM

✅ Thành công:
```
🔍 Safety Check - MAC: F4DD40BA2010, fallDetected: 0, helpRequest: 1
⚡ Alert Check - fallDetected=0, helpRequest=1
🆘 HELP REQUEST - Creating alert...
🆘 createHelpRequestAlert() called for MAC: F4DD40BA2010
✅ Creating HELP_REQUEST alert...
💾 Saving HELP_REQUEST alert to database...
✅ HELP_REQUEST alert saved successfully - ID: 76, Type: HELP_REQUEST, Severity: CRITICAL
📡 HELP_REQUEST alert published via WebSocket
🆘 HELP REQUEST ALERT CREATED: ... at (...)
```

❌ Lỗi debounce:
```
⏭️ Skip duplicate help request alert (debounce: 15s since last)
```

❌ Lỗi database:
```
❌ CRITICAL: Failed to save HELP_REQUEST alert to database
java.sql.SQLException: ...
```
