# 🚀 MQTT IoT Integration - Hướng dẫn triển khai

## ✅ Đã hoàn thành

### 1. **Cấu hình MQTT** (`application.properties`)
```properties
mqtt.broker.url=ssl://d0a82f39864c4e86a0551feaed97f7c5.s1.eu.hivemq.cloud:8883
mqtt.client.id=bfe-backend-${random.value}
mqtt.username=truong123
mqtt.password=Truong123
mqtt.topic=helmet/#
```

### 2. **Database Schema** (`schema.sql`)
- ✅ Bảng `employees` - Quản lý nhân viên và MAC address helmet
- ✅ Bảng `helmet_data` - Lưu dữ liệu từ MQTT
- ✅ Bảng `messenger_users` - Quản lý người dùng Messenger

### 3. **Backend Components**

#### **Entity**
- ✅ `Employee.java` - Entity nhân viên với trường `macAddress`
- ✅ `HelmetData.java` - Entity lưu dữ liệu helmet từ MQTT

#### **Repository**
- ✅ `EmployeeRepository.java` - Query nhân viên theo MAC
- ✅ `HelmetDataRepository.java` - Lưu dữ liệu helmet

#### **Config**
- ✅ `MqttConfig.java` - Kết nối HiveMQ Cloud, subscribe topic `helmet/#`

#### **Service**
- ✅ `MqttMessageHandler.java` - Xử lý message từ MQTT:
  - Parse JSON từ ESP32
  - Map MAC → Employee
  - Lưu database
  - Kiểm tra ngưỡng nguy hiểm
  - Gửi cảnh báo qua Messenger

#### **Controller**
- ✅ `EmployeeController.java` - REST API quản lý nhân viên:
  - `GET /api/employees` - Lấy danh sách
  - `POST /api/employees` - Tạo nhân viên
  - `PUT /api/employees/{id}` - Cập nhật
  - `DELETE /api/employees/{id}` - Xóa
  - `PUT /api/employees/{id}/assign-mac` - Gán MAC address

### 4. **Frontend**
- ✅ `manage-employees.html` - Giao diện quản lý nhân viên & MAC address

---

## 📊 Luồng dữ liệu

```
ESP32 Helmet (MAC: A48D004AEC24)
    ↓
Raspberry Pi Gateway (Python + paho-mqtt)
    ↓
HiveMQ Cloud (ssl://d0a82f39864c4e86a0551feaed97f7c5.s1.eu.hivemq.cloud:8883)
    ↓ Topic: helmet/A48D004AEC24
Spring Boot Backend (MqttConfig + MqttMessageHandler)
    ↓
1. Parse JSON: {mac, voltage, current, power, battery, lat, lon, counter, timestamp}
2. Query Employee by MAC address
3. Save to MySQL (helmet_data table)
4. Check thresholds:
   - Battery < 20% → Alert
   - Voltage < 10V → Alert  
   - Current > 50A → Alert
5. Broadcast alert via Messenger (MessengerService)
```

---

## 🎯 Ngưỡng cảnh báo hiện tại

```java
// MqttMessageHandler.java
private static final double BATTERY_LOW_THRESHOLD = 20.0;    // Pin < 20%
private static final double VOLTAGE_LOW_THRESHOLD = 10.0;    // Điện áp < 10V
private static final double CURRENT_HIGH_THRESHOLD = 50.0;   // Dòng điện > 50A
```

---

## 🔧 Các bước triển khai

### **Bước 1: Tạo nhân viên và gán MAC**

1. Truy cập: `http://localhost:8080/manage-employees.html`
2. Thêm nhân viên mới:
   - Mã NV: `NV001`
   - Họ tên: `Nguyễn Văn An`
   - MAC Address: `A48D004AEC24` (từ ESP32)
3. Hoặc gán MAC sau bằng nút "🔗 Gán MAC"

### **Bước 2: Deploy lên Heroku**

```bash
# Build project
mvn clean package -DskipTests

# Deploy to Heroku
git add .
git commit -m "Add MQTT integration with employee mapping"
git push heroku main
```

### **Bước 3: Chạy Raspberry Pi Gateway**

Đảm bảo Raspberry Pi đang chạy code Python với cấu hình:
```python
MQTT_BROKER = "d0a82f39864c4e86a0551feaed97f7c5.s1.eu.hivemq.cloud"
MQTT_PORT = 8883
MQTT_USERNAME = "truong123"
MQTT_PASSWORD = "Truong123"
MQTT_TOPIC = f"helmet/{MAC_ADDRESS}"
```

### **Bước 4: Test cảnh báo**

1. ESP32 gửi data với battery < 20%
2. Backend nhận qua MQTT
3. Kiểm tra ngưỡng → Phát hiện nguy hiểm
4. Gửi alert qua Messenger cho users đã subscribe

---

## 📋 REST API Endpoints

### **Quản lý Nhân viên**

#### 1. Lấy danh sách nhân viên
```http
GET /api/employees
```

Response:
```json
[
  {
    "employeeId": "NV001",
    "name": "Nguyễn Văn An",
    "position": "Công nhân",
    "department": "Sản xuất",
    "macAddress": "A48D004AEC24",
    "phoneNumber": "0901234567",
    "email": "an.nv@company.com",
    "status": "ACTIVE"
  }
]
```

#### 2. Tạo nhân viên mới
```http
POST /api/employees
Content-Type: application/json

{
  "employeeId": "NV002",
  "name": "Trần Thị Bình",
  "position": "Kỹ sư",
  "department": "Kỹ thuật",
  "macAddress": "B58D004AEC25",
  "phoneNumber": "0902345678",
  "email": "binh.tt@company.com",
  "status": "ACTIVE"
}
```

#### 3. Gán MAC address
```http
PUT /api/employees/NV001/assign-mac
Content-Type: application/json

{
  "macAddress": "A48D004AEC24"
}
```

#### 4. Cập nhật nhân viên
```http
PUT /api/employees/NV001
Content-Type: application/json

{
  "name": "Nguyễn Văn An",
  "position": "Trưởng ca",
  "department": "Sản xuất",
  "macAddress": "A48D004AEC24",
  "phoneNumber": "0901234567",
  "email": "an.nv@company.com",
  "status": "ACTIVE"
}
```

#### 5. Xóa nhân viên
```http
DELETE /api/employees/NV001
```

---

## 🧪 Test MQTT locally

### **Test 1: Subscribe to MQTT topic**
```bash
# Install MQTT client
npm install -g mqtt

# Subscribe to all helmet topics
mqtt subscribe -h d0a82f39864c4e86a0551feaed97f7c5.s1.eu.hivemq.cloud \
  -p 8883 \
  -u truong123 \
  -P Truong123 \
  --protocol mqtts \
  -t 'helmet/#'
```

### **Test 2: Publish test data**
```bash
mqtt publish -h d0a82f39864c4e86a0551feaed97f7c5.s1.eu.hivemq.cloud \
  -p 8883 \
  -u truong123 \
  -P Truong123 \
  --protocol mqtts \
  -t 'helmet/A48D004AEC24' \
  -m '{"mac":"A48D004AEC24","voltage":11.58,"current":-33.3,"power":390.0,"battery":15.0,"lat":10.762400,"lon":106.660050,"counter":1,"timestamp":"2025-11-10T01:00:00"}'
```

Expected: Backend nhận message, lưu DB, gửi alert (battery 15% < 20%)

---

## 📂 Cấu trúc Database

### **Table: employees**
```sql
CREATE TABLE employees (
    employee_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    position VARCHAR(100),
    department VARCHAR(100),
    mac_address VARCHAR(20) UNIQUE,  -- MAC của helmet
    phone_number VARCHAR(20),
    email VARCHAR(255),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### **Table: helmet_data**
```sql
CREATE TABLE helmet_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mac VARCHAR(20) NOT NULL,
    voltage DOUBLE,
    current DOUBLE,
    power DOUBLE,
    battery DOUBLE,
    lat DOUBLE,
    lon DOUBLE,
    counter INT,
    employee_id VARCHAR(50),
    employee_name VARCHAR(255),
    timestamp TIMESTAMP,
    INDEX idx_mac (mac),
    INDEX idx_employee_id (employee_id),
    INDEX idx_timestamp (timestamp)
);
```

---

## ⚙️ Customize ngưỡng cảnh báo

Sửa file `MqttMessageHandler.java`:

```java
// Tùy chỉnh ngưỡng
private static final double BATTERY_LOW_THRESHOLD = 20.0;     // Pin < 20%
private static final double VOLTAGE_LOW_THRESHOLD = 10.0;     // Điện áp < 10V
private static final double CURRENT_HIGH_THRESHOLD = 50.0;    // Dòng điện > 50A

// Thêm ngưỡng mới
private static final double POWER_HIGH_THRESHOLD = 500.0;     // Công suất > 500W

// Kiểm tra trong method checkDangerAndAlert()
if (data.getPower() != null && data.getPower() > POWER_HIGH_THRESHOLD) {
    alertMessage.append(String.format("⚠️ Công suất cao: %.2fW\n", data.getPower()));
    isDangerous = true;
}
```

---

## 🔍 Troubleshooting

### **Lỗi: Backend không nhận MQTT message**
1. Kiểm tra logs: `heroku logs --tail`
2. Xem log MQTT connection: `🔗 MQTT Client Factory initialized`
3. Xem log subscribe: `📡 MQTT Subscriber created for topic: helmet/#`

### **Lỗi: Không map được Employee**
1. Kiểm tra MAC address trong database có đúng không
2. Log hiển thị: `⚠️ No employee found for MAC: A48D004AEC24`
3. Thêm employee với MAC đúng qua API hoặc manage-employees.html

### **Lỗi: Không gửi được Messenger alert**
1. Kiểm tra MessengerUser có subscribed=true không
2. Kiểm tra Page Access Token còn hợp lệ
3. Xem log: `🚨 Danger alert broadcasted for MAC: ...`

---

## 📱 URLs quan trọng

- **Heroku App**: https://edl-safework-iot-bf3ee691c9f6.herokuapp.com
- **Quản lý nhân viên**: https://edl-safework-iot-bf3ee691c9f6.herokuapp.com/manage-employees.html
- **Test cảnh báo**: https://edl-safework-iot-bf3ee691c9f6.herokuapp.com/test-alert.html
- **HiveMQ Cloud Console**: https://console.hivemq.cloud

---

## 🎉 Tính năng đã triển khai

✅ Kết nối MQTT HiveMQ Cloud  
✅ Subscribe topic `helmet/#` (wildcard all helmets)  
✅ Parse JSON data từ ESP32  
✅ Map MAC address → Employee  
✅ Lưu dữ liệu vào MySQL  
✅ Kiểm tra ngưỡng nguy hiểm (battery, voltage, current)  
✅ Gửi cảnh báo qua Facebook Messenger  
✅ REST API quản lý nhân viên  
✅ Web UI quản lý nhân viên & MAC address  
✅ Database schema hoàn chỉnh  

---

## 📝 Notes

- MAC address format: 12 ký tự hex (VD: `A48D004AEC24`)
- Timestamp từ ESP32 format: ISO 8601 (`2025-11-10T00:55:30.016286`)
- MQTT QoS: 1 (At least once delivery)
- SSL/TLS: Enabled (port 8883)

---

**Tác giả**: GitHub Copilot  
**Ngày tạo**: 2025-11-10  
**Version**: 1.0.0
