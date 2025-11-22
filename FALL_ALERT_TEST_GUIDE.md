# 🧪 Hướng Dẫn Test Hệ Thống Cảnh Báo Ngã & SOS

## 📋 Tổng Quan

Hệ thống đã được cập nhật để xử lý 2 loại cảnh báo khẩn cấp từ mũ bảo hộ:
- **fallDetected: 1** → 🚨 Phát hiện ngã
- **helpRequest: 1** → 🆘 Yêu cầu trợ giúp (SOS)

## 🔄 Luồng Xử Lý

```
1. Mũ bảo hộ phát hiện ngã/SOS
   ↓
2. ESP32 gửi MQTT message → HiveMQ Cloud
   Topic: helmet/A48D004AEC24
   Payload: {"mac": "A48D004AEC24", "fallDetected": 1, ...}
   ↓
3. Backend nhận MQTT → MqttMessageHandler
   ↓
4. Xử lý:
   - Lưu vào helmet_data (với employee_id, employee_name)
   - Tạo Alert record (severity: CRITICAL)
   - Gửi Messenger notification
   - Push WebSocket realtime
   ↓
5. Frontend (alerts.html):
   - Nhận WebSocket → Hiện alert mới
   - Play sound notification
   - Cập nhật số liệu thống kê
```

## ✅ Checklist Chuẩn Bị

### 1. Đảm Bảo Có Employee với MAC
```sql
-- Kiểm tra
SELECT employee_id, name, mac_address 
FROM employees 
WHERE mac_address = 'A48D004AEC24';

-- Nếu chưa có, thêm employee
INSERT INTO employees (employee_id, name, position, mac_address, status, created_at)
VALUES ('TEST01', 'Nguyễn Văn Test', 'Công nhân', 'A48D004AEC24', 'ACTIVE', NOW());
```

### 2. Kiểm Tra Backend Đang Chạy
```powershell
# Check application logs
tail -f logs/application.log

# Hoặc xem console output
# Tìm dòng: "📩 Received MQTT from topic: helmet/A48D004AEC24"
```

### 3. Mở Trang alerts.html
```
http://localhost:8080/alerts.html
hoặc
https://your-app.herokuapp.com/alerts.html
```

## 🧪 Các Bước Test

### Test 1: Gửi Cảnh Báo NGÃ (Fall Detected)

**1. Chạy script test:**
```powershell
cd J:\IOT\BFE_forAIOT
.\test-fall-alert.ps1
# Chọn option: 1
```

**2. Message MQTT được gửi:**
```json
{
  "mac": "A48D004AEC24",
  "temp": 36.5,
  "voltage": 8.22,
  "current": -0.0,
  "battery": 95.0,
  "lat": 10.762400,
  "lon": 106.660050,
  "hr": 75.0,
  "spo2": 98.0,
  "uwb": {
    "A0": 2.09,
    "A1": 2.02,
    "TAG2": 4.26,
    "A2": 3.58,
    "baseline_A1": 0.99,
    "baseline_A2": 1.52,
    "ready": 1
  },
  "fallDetected": 1,
  "helpRequest": 0,
  "timestamp": "2025-11-23T10:30:00.000000"
}
```

**3. Kiểm tra Backend Log:**
```
📩 Received MQTT from topic: helmet/A48D004AEC24
👤 MAC A48D004AEC24 → Employee: Nguyễn Văn Test (TEST01)
✅ SAVE: MAC=A48D004AEC24, Mode=direct, Battery=95.0%, Loc=(10.762400,106.660050)
🚨 FALL DETECTED: Nguyễn Văn Test (TEST01) at (10.762400, 106.660050)
📡 Alert published to WebSocket: /topic/alerts/new
```

**4. Kiểm tra Database:**
```sql
-- Alerts table
SELECT * FROM alerts 
WHERE helmet_id = (SELECT id FROM helmets WHERE mac_address = 'A48D004AEC24')
ORDER BY triggered_at DESC 
LIMIT 1;

-- Kết quả mong đợi:
-- alert_type: FALL
-- severity: CRITICAL
-- status: PENDING
-- message: 🚨 PHÁT HIỆN NGÃ: Nguyễn Văn Test (TEST01)
-- gps_lat: 10.762400
-- gps_lon: 106.660050
```

**5. Kiểm tra alerts.html:**
- ✅ Popup notification: "Cảnh báo mới: Phát hiện sự cố"
- ✅ Sound notification (nếu allowed)
- ✅ Dòng mới xuất hiện trong bảng:
  ```
  Thời gian: 23/11/2025 10:30
  Công nhân: Helmet-xxx
  Loại: 🚨 Phát hiện ngã
  Mức độ: Nghiêm trọng (màu đỏ)
  Trạng thái: Chờ xử lý
  ```
- ✅ Số liệu thống kê cập nhật:
  - Tổng cảnh báo: +1
  - Chờ xử lý: +1
  - Nghiêm trọng: +1

### Test 2: Gửi Cảnh Báo SOS (Help Request)

**1. Chạy script:**
```powershell
.\test-fall-alert.ps1
# Chọn option: 2
```

**2. Message MQTT:**
```json
{
  "mac": "A48D004AEC24",
  "battery": 92.0,
  "lat": 10.762600,
  "lon": 106.660150,
  "fallDetected": 0,
  "helpRequest": 1,
  ...
}
```

**3. Kiểm tra:**
- Backend log: `🆘 HELP REQUEST: Nguyễn Văn Test (TEST01) at (...)`
- Database: `alert_type = HELP_REQUEST`, `message = 🆘 YÊU CẦU TRỢ GIÚP`
- alerts.html: Loại = "🆘 Yêu cầu trợ giúp"

### Test 3: Cảnh Báo Kép (Fall + SOS)

**1. Chạy script:**
```powershell
.\test-fall-alert.ps1
# Chọn option: 3
```

**2. Message MQTT:**
```json
{
  "fallDetected": 1,
  "helpRequest": 1,
  ...
}
```

**3. Kết quả mong đợi:**
- **2 alerts** được tạo:
  1. Alert FALL
  2. Alert HELP_REQUEST
- Cả 2 đều có severity = CRITICAL
- Frontend hiện 2 notification
- Sound play 2 lần

### Test 4: Auto Test (Tất Cả Scenarios)

```powershell
.\test-fall-alert.ps1
# Chọn option: 5
```

Sẽ gửi 4 message liên tiếp:
1. ✅ Normal (không có alert)
2. 🚨 Fall
3. 🆘 SOS
4. 🚨🆘 Cả 2

## 📊 Test Chức Năng alerts.html

### 1. Xác Nhận Alert (Acknowledge)
```javascript
// Click nút "Xác nhận" (check icon)
// → Alert status: PENDING → ACKNOWLEDGED
// → acknowledged_at: current time
// → acknowledged_by: 'Admin'
```

**Kiểm tra:**
```sql
SELECT status, acknowledged_at, acknowledged_by 
FROM alerts 
WHERE id = <alert_id>;
```

### 2. Giải Quyết Alert (Resolve)
```javascript
// Click nút "Giải quyết" (double-check icon)
// → Alert status: ACKNOWLEDGED → RESOLVED
```

### 3. Xem Chi Tiết Alert
```javascript
// Click nút "Xem" (eye icon)
// → Popup hiện thông tin đầy đủ
// → Option: "Xem vị trí trên bản đồ"
// → Redirect to location.html?helmetId=xxx
```

### 4. Lọc & Tìm Kiếm
- **Filter by Status:** Chờ xử lý / Đã xác nhận / Đã giải quyết
- **Filter by Severity:** Nghiêm trọng / Cảnh báo / Thông tin
- **Search:** Tìm theo ID, Công nhân, Loại cảnh báo

## 🔍 Troubleshooting

### Vấn đề 1: Không nhận được alert trên frontend

**Nguyên nhân:** WebSocket chưa kết nối

**Giải pháp:**
1. Mở Developer Console (F12)
2. Tìm log:
   ```
   🔌 Connecting Alerts WebSocket...
   ✅ Alerts WebSocket connected!
   ```
3. Nếu lỗi, check backend có enable WebSocket không

### Vấn đề 2: Alert được tạo nhưng không hiện employee_id

**Nguyên nhân:** MAC chưa map với employee

**Giải pháp:**
```sql
-- Check mapping
SELECT * FROM employees WHERE mac_address = 'A48D004AEC24';

-- Nếu NULL, update
UPDATE employees 
SET mac_address = 'A48D004AEC24' 
WHERE employee_id = 'TEST01';
```

### Vấn đề 3: Duplicate alerts khi gửi nhiều lần

**Lý do:** Đúng! Mỗi message MQTT sẽ tạo 1 alert mới

**Giải pháp (nếu cần):**
```java
// Thêm debounce trong MqttMessageHandler
private final Map<String, LocalDateTime> lastFallAlert = new HashMap<>();

// Check before creating alert
LocalDateTime lastAlert = lastFallAlert.get(macAddress);
if (lastAlert != null && Duration.between(lastAlert, now).getSeconds() < 30) {
    log.debug("Skip duplicate fall alert (debounce)");
    return;
}
```

## 📈 Query Monitoring

### Alerts trong 24h qua
```sql
SELECT 
    a.id,
    a.alert_type,
    a.severity,
    a.status,
    a.message,
    a.triggered_at,
    h.mac_address,
    w.full_name AS worker_name
FROM alerts a
LEFT JOIN helmets h ON a.helmet_id = h.id
LEFT JOIN workers w ON h.worker_id = w.id
WHERE a.triggered_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
ORDER BY a.triggered_at DESC;
```

### Thống kê alerts theo loại
```sql
SELECT 
    alert_type,
    COUNT(*) AS total,
    SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) AS pending,
    SUM(CASE WHEN status = 'ACKNOWLEDGED' THEN 1 ELSE 0 END) AS acknowledged,
    SUM(CASE WHEN status = 'RESOLVED' THEN 1 ELSE 0 END) AS resolved
FROM alerts
WHERE triggered_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY alert_type
ORDER BY total DESC;
```

### Top workers với nhiều alerts nhất
```sql
SELECT 
    w.employee_id,
    w.full_name,
    COUNT(a.id) AS alert_count,
    SUM(CASE WHEN a.alert_type = 'FALL' THEN 1 ELSE 0 END) AS fall_count,
    SUM(CASE WHEN a.alert_type = 'HELP_REQUEST' THEN 1 ELSE 0 END) AS sos_count
FROM alerts a
JOIN helmets h ON a.helmet_id = h.id
JOIN workers w ON h.worker_id = w.id
WHERE a.triggered_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY w.id, w.employee_id, w.full_name
ORDER BY alert_count DESC
LIMIT 10;
```

## ✅ Test Checklist Hoàn Chỉnh

- [ ] Employee có MAC address đúng trong database
- [ ] Backend đang chạy và nhận được MQTT
- [ ] Gửi Fall Detected → Alert FALL được tạo
- [ ] Gửi Help Request → Alert HELP_REQUEST được tạo
- [ ] Gửi cả 2 → Cả 2 alerts được tạo
- [ ] alerts.html hiện realtime notification
- [ ] Sound notification hoạt động
- [ ] Bảng alerts cập nhật realtime
- [ ] Số liệu thống kê cập nhật đúng
- [ ] Filter & Search hoạt động
- [ ] Acknowledge alert thành công
- [ ] Resolve alert thành công
- [ ] View alert detail → redirect to map
- [ ] Messenger notification được gửi (nếu config)
- [ ] Database có records đúng

## 🎯 Expected Results

Sau khi test xong, bạn sẽ có:

1. **Database alerts table:**
   - Nhiều records với alert_type = FALL, HELP_REQUEST
   - Severity = CRITICAL
   - Status = PENDING / ACKNOWLEDGED / RESOLVED
   - GPS coordinates đầy đủ

2. **Frontend alerts.html:**
   - Realtime updates qua WebSocket
   - Notification popups
   - Sound alerts
   - Updated statistics
   - Functional filters

3. **Backend logs:**
   - MQTT messages received
   - Alerts created
   - WebSocket published
   - Messenger sent (if configured)

## 📝 Notes

- **fallDetected = 1:** Mũ phát hiện ngã qua cảm biến gia tốc (MPU-9250)
- **helpRequest = 1:** Công nhân nhấn nút SOS trên mũ
- **Cả 2 = 1:** Tình huống khẩn cấp nghiêm trọng
- **Debounce:** Backend có thể cần debounce để tránh spam alerts
- **Realtime:** WebSocket push ngay lập tức, không cần refresh

---
✅ **Hệ thống sẵn sàng xử lý cảnh báo ngã và SOS từ dữ liệu thực tế!**
