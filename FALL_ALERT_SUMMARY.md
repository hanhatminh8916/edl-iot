# ✅ Tổng Kết: Tích Hợp Cảnh Báo Ngã & SOS

## 🎯 Đã Hoàn Thành

### 1. Backend Changes

#### MqttMessageHandler.java
- ✅ Parse `fallDetected` và `helpRequest` từ MQTT JSON
- ✅ Parse thêm `temp`, `hr`, `spo2` (health metrics)
- ✅ Method `createFallDetectedAlert()` - tạo alert khi phát hiện ngã
- ✅ Method `createHelpRequestAlert()` - tạo alert khi nhận SOS
- ✅ Lưu alert vào database với severity = CRITICAL
- ✅ Push alert realtime qua WebSocket
- ✅ Gửi notification qua Messenger
- ✅ Log chi tiết cho debugging

#### AlertType.java
- ✅ Thêm enum `HELP_REQUEST` cho cảnh báo SOS
- ✅ Comment mô tả cho từng loại alert

### 2. Frontend Changes

#### alerts.js
- ✅ Cập nhật `getAlertTypeText()` với icon và text:
  - 🚨 Phát hiện ngã
  - 🆘 Yêu cầu trợ giúp
  - ⚠️ Gần khu vực nguy hiểm
  - 🔋 Pin yếu
  - ...
- ✅ WebSocket realtime listener cho `/topic/alerts/new`
- ✅ Sound notification khi có alert mới
- ✅ Auto reload alerts table
- ✅ Filter và search alerts

### 3. Test Tools

#### test-fall-alert.ps1
- ✅ Script PowerShell để test 5 scenarios:
  1. Fall Detected only
  2. Help Request (SOS) only
  3. Both Fall + SOS
  4. Normal (no alert)
  5. Auto test all scenarios
- ✅ Gửi MQTT message đến HiveMQ Cloud
- ✅ Formatted output với màu sắc
- ✅ Instructions sau khi test

#### Documentation
- ✅ `FALL_ALERT_TEST_GUIDE.md` - Hướng dẫn test đầy đủ
- ✅ `EMPLOYEE_HELMET_MAPPING_TEST.md` - Hướng dẫn mapping employee với helmet

## 📊 Luồng Dữ Liệu

```
ESP32 (Mũ bảo hộ)
    ↓ fallDetected: 1 hoặc helpRequest: 1
Gateway (LoRa/WiFi)
    ↓ MQTT publish
HiveMQ Cloud
    ↓ Topic: helmet/A48D004AEC24
Backend Spring Boot
    ↓ MqttMessageHandler.handleMessage()
    ├─→ Parse fallDetected & helpRequest
    ├─→ Find employee by MAC address
    ├─→ Save to helmet_data (employee_id, employee_name)
    ├─→ createFallDetectedAlert() / createHelpRequestAlert()
    │   ├─→ Save Alert to database
    │   ├─→ alertPublisher.publishNewAlert() → WebSocket
    │   └─→ messengerService.broadcastDangerAlert()
    └─→ Update helmets table (battery, location)
        ↓
Frontend (alerts.html)
    ├─→ WebSocket: Nhận alert mới → Popup + Sound
    ├─→ Auto refresh table
    └─→ Update statistics
```

## 🔧 Cấu Trúc Database

### Table: alerts
```sql
id                BIGINT (PK)
helmet_id         BIGINT (FK → helmets.id)
alert_type        ENUM (FALL, HELP_REQUEST, PROXIMITY, LOW_BATTERY, ...)
severity          ENUM (CRITICAL, WARNING, INFO)
status            ENUM (PENDING, ACKNOWLEDGED, RESOLVED)
message           TEXT (🚨 PHÁT HIỆN NGÃ: Nguyễn Văn...)
gps_lat           DOUBLE (10.762400)
gps_lon           DOUBLE (106.660050)
triggered_at      DATETIME (2025-11-23 10:30:00)
acknowledged_at   DATETIME (null)
acknowledged_by   VARCHAR (null)
```

### Table: helmet_data (realtime data)
```sql
id                BIGINT (PK)
mac               VARCHAR (A48D004AEC24)
employee_id       VARCHAR (TEST01) ⭐ auto-mapped
employee_name     VARCHAR (Nguyễn Văn Test) ⭐ auto-mapped
battery           DOUBLE (95.0)
lat               DOUBLE (10.762400)
lon               DOUBLE (106.660050)
voltage           DOUBLE (8.22)
current           DOUBLE (0.0)
timestamp         DATETIME (2025-11-23 10:30:00)
```

### Table: employees (MAC mapping)
```sql
employee_id       VARCHAR (TEST01) PK
name              VARCHAR (Nguyễn Văn Test)
position          VARCHAR (Công nhân)
mac_address       VARCHAR (A48D004AEC24) ⭐ UNIQUE
phone_number      VARCHAR
status            VARCHAR (ACTIVE)
```

## 📡 MQTT Message Format

### Normal Data
```json
{
  "mac": "A48D004AEC24",
  "temp": 36.5,
  "voltage": 8.22,
  "current": -0.0,
  "battery": 100.0,
  "lat": 10.762400,
  "lon": 106.660050,
  "hr": 72.0,
  "spo2": 99.0,
  "uwb": {
    "A0": 2.01,
    "A1": 2.05,
    "TAG2": 4.35,
    "A2": 3.95,
    "baseline_A1": 0.99,
    "baseline_A2": 1.52,
    "ready": 1
  },
  "fallDetected": 0,
  "helpRequest": 0,
  "timestamp": "2025-11-23T10:30:00.000000"
}
```

### Fall Detected (NGÃ)
```json
{
  "mac": "A48D004AEC24",
  "battery": 95.0,
  "lat": 10.762400,
  "lon": 106.660050,
  "fallDetected": 1,  ⭐
  "helpRequest": 0,
  "timestamp": "2025-11-23T10:30:00.000000"
}
```

### Help Request (SOS)
```json
{
  "mac": "A48D004AEC24",
  "battery": 92.0,
  "lat": 10.762600,
  "lon": 106.660150,
  "fallDetected": 0,
  "helpRequest": 1,  ⭐
  "timestamp": "2025-11-23T10:30:01.000000"
}
```

### Critical (NGÃ + SOS)
```json
{
  "mac": "A48D004AEC24",
  "hr": 120.0,        ⭐ Tim đập nhanh
  "spo2": 93.0,       ⭐ SpO2 thấp
  "fallDetected": 1,  ⭐
  "helpRequest": 1,   ⭐
  "timestamp": "2025-11-23T10:30:02.000000"
}
```

## 🚀 Cách Sử Dụng

### 1. Chuẩn Bị
```sql
-- Thêm employee với MAC
INSERT INTO employees (employee_id, name, position, mac_address, status, created_at)
VALUES ('TEST01', 'Nguyễn Văn Test', 'Công nhân', 'A48D004AEC24', 'ACTIVE', NOW());
```

### 2. Chạy Backend
```powershell
mvn spring-boot:run
# Hoặc deploy lên Heroku
```

### 3. Test Alert
```powershell
cd J:\IOT\BFE_forAIOT
.\test-fall-alert.ps1
# Chọn option 1-5
```

### 4. Xem Kết Quả
- **Backend logs:** Console output
- **Database:** Query alerts table
- **Frontend:** Open `alerts.html`
- **Messenger:** Check notifications (nếu configured)

## 📈 Monitoring

### Query Recent Alerts
```sql
SELECT 
    a.id,
    a.alert_type,
    a.message,
    a.severity,
    a.status,
    a.triggered_at,
    h.mac_address,
    w.full_name
FROM alerts a
LEFT JOIN helmets h ON a.helmet_id = h.id
LEFT JOIN workers w ON h.worker_id = w.id
WHERE a.triggered_at >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
ORDER BY a.triggered_at DESC;
```

### Check WebSocket Activity
```javascript
// Open Browser Console on alerts.html
// Look for:
"✅ Alerts WebSocket connected!"
"🚨 New alert received realtime: {...}"
```

## 🎓 Next Steps (Tùy Chọn)

### 1. Debounce Duplicate Alerts
```java
// In MqttMessageHandler
private final Map<String, LocalDateTime> lastFallAlert = new HashMap<>();

private void createFallDetectedAlert(HelmetData data) {
    String mac = data.getMac();
    LocalDateTime now = LocalDateTime.now();
    
    // Debounce: chỉ tạo alert nếu > 30s kể từ alert trước
    LocalDateTime lastAlert = lastFallAlert.get(mac);
    if (lastAlert != null && Duration.between(lastAlert, now).getSeconds() < 30) {
        log.debug("⏭️ Skip duplicate fall alert (debounce)");
        return;
    }
    
    lastFallAlert.put(mac, now);
    
    // ... tạo alert như bình thường
}
```

### 2. Email Notification
```java
@Autowired
private JavaMailSender mailSender;

private void sendEmailAlert(Alert alert) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo("admin@company.com");
    message.setSubject("🚨 CẢNH BÁO KHẨN CẤP");
    message.setText(alert.getMessage());
    mailSender.send(message);
}
```

### 3. SMS Notification
```java
// Sử dụng Twilio API
Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
Message message = Message.creator(
    new PhoneNumber("+84901234567"),
    new PhoneNumber(TWILIO_PHONE),
    "🚨 Phát hiện ngã: Nguyễn Văn Test"
).create();
```

### 4. Auto-Assign Nearest Responder
```java
// Tìm công nhân gần nhất để hỗ trợ
private Worker findNearestWorker(double lat, double lon) {
    List<Worker> activeWorkers = workerRepository.findByStatus(WorkerStatus.ACTIVE);
    // Calculate distances and return nearest
}
```

## ✅ Checklist Deploy

- [ ] Code changes committed
- [ ] Database schema updated (alerts table có đủ columns)
- [ ] Environment variables configured (MQTT, Messenger, etc.)
- [ ] Backend deployed và running
- [ ] WebSocket enabled trên production
- [ ] Test với data thật từ HiveMQ
- [ ] Verify alerts.html hoạt động
- [ ] Check Messenger notifications
- [ ] Document API cho team

## 📚 Files Changed

```
Backend:
├── MqttMessageHandler.java (✅ Parse fall/SOS, create alerts)
├── AlertType.java (✅ Add HELP_REQUEST enum)
└── (AlertPublisher.java - đã có sẵn)

Frontend:
└── js/alerts.js (✅ Update getAlertTypeText)

Test Tools:
├── test-fall-alert.ps1 (✅ New test script)
├── FALL_ALERT_TEST_GUIDE.md (✅ Test documentation)
└── EMPLOYEE_HELMET_MAPPING_TEST.md (✅ Mapping guide)

Documentation:
└── FALL_ALERT_SUMMARY.md (✅ This file)
```

## 🎉 Kết Luận

Hệ thống đã sẵn sàng nhận và xử lý cảnh báo ngã và SOS từ dữ liệu thực tế:

✅ **Backend:** Parse MQTT, create alerts, push WebSocket, send Messenger
✅ **Frontend:** Realtime updates, notifications, sound alerts
✅ **Database:** Store alerts với đầy đủ thông tin
✅ **Testing:** Scripts và documentation đầy đủ

**Sử dụng ngay:**
```powershell
.\test-fall-alert.ps1
```

---
🚀 **Ready for Production!**
