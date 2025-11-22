# 🧪 Hướng dẫn Test: Gán Mũ cho Công Nhân & Hiển thị trên Bản đồ

## 📋 Tổng Quan Hệ Thống

### Luồng Dữ Liệu
```
1. Thêm công nhân (employees.html)
   ↓
2. Gán MAC address của mũ
   ↓
3. Backend lưu vào 2 bảng:
   - workers (quản lý công nhân)
   - employees (mapping MAC → employee_id, employee_name)
   ↓
4. Mũ gửi dữ liệu MQTT → HiveMQ Cloud
   ↓
5. Backend nhận MQTT:
   - Tìm employee theo MAC address
   - Lưu vào helmet_data với employee_id & employee_name
   - Cập nhật bảng helmets (battery, location, status)
   ↓
6. Map (location.html) hiển thị:
   - Đọc employees + helmet_data mới nhất
   - Hiện tên, vị trí, pin công nhân
```

## ✅ Các Bước Test

### Bước 1: Thêm Công Nhân Mới
1. Mở trang `employees.html`
2. Click nút **"Thêm công nhân"**
3. Điền thông tin:
   ```
   Họ tên: Nguyễn Văn Test
   Số điện thoại: 0901234567
   Chức vụ: Công nhân
   Khu vực: 1 - Khu đông
   Mũ bảo hiểm: Chọn mũ có MAC A48D004AEC24
   ```
4. Click **"Lưu thông tin"**

### Bước 2: Kiểm tra Database
Mở database và kiểm tra:

**Bảng `workers`:**
```sql
SELECT * FROM workers WHERE full_name LIKE '%Test%';
-- Kết quả mong đợi:
-- employee_id: REVxx (tự động tạo)
-- full_name: Nguyễn Văn Test
-- phone_number: 0901234567
-- position: Công nhân
```

**Bảng `employees`:**
```sql
SELECT * FROM employees WHERE name LIKE '%Test%';
-- Kết quả mong đợi:
-- employee_id: REVxx (giống bảng workers)
-- name: Nguyễn Văn Test
-- mac_address: A48D004AEC24 ⭐ (quan trọng!)
```

**Bảng `helmets`:**
```sql
SELECT h.*, w.full_name 
FROM helmets h 
LEFT JOIN workers w ON h.worker_id = w.id
WHERE h.mac_address = 'A48D004AEC24';
-- Kết quả mong đợi:
-- worker_id: <ID của Nguyễn Văn Test>
-- mac_address: A48D004AEC24
```

### Bước 3: Gửi Dữ Liệu MQTT từ Mũ
Sử dụng MQTT client hoặc test script để gửi dữ liệu:

**Topic:** `helmet/A48D004AEC24`

**Payload (JSON):**
```json
{
  "mac": "A48D004AEC24",
  "temp": 36.5,
  "voltage": 8.22,
  "current": -0.0,
  "battery": 100.0,
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
  "fallDetected": 0,
  "helpRequest": 0,
  "timestamp": "2025-11-23T10:30:00"
}
```

**Sử dụng MQTT CLI:**
```bash
mqtt publish \
  -h d0a82f39864c4e86a0551feaed97f7c5.s1.eu.hivemq.cloud \
  -p 8883 \
  -u truong123 \
  -P Truong123 \
  --protocol mqtts \
  -t "helmet/A48D004AEC24" \
  -m '{"mac":"A48D004AEC24","temp":36.5,"voltage":8.22,"current":0,"battery":100.0,"lat":10.762400,"lon":106.660050,"hr":75.0,"spo2":98.0,"uwb":{"A0":2.09,"A1":2.02,"TAG2":4.26,"A2":3.58,"baseline_A1":0.99,"baseline_A2":1.52,"ready":1},"fallDetected":0,"helpRequest":0,"timestamp":"2025-11-23T10:30:00"}'
```

### Bước 4: Kiểm tra Backend Log
Xem console log của Spring Boot application:

```
📩 Received MQTT from topic: helmet/A48D004AEC24
👤 MAC A48D004AEC24 → Employee: Nguyễn Văn Test (REVxx)
✅ SAVE: MAC=A48D004AEC24, Mode=direct, Battery=100.0%, Loc=(10.762400,106.660050)
```

### Bước 5: Kiểm tra Database helmet_data
```sql
SELECT * FROM helmet_data 
WHERE mac = 'A48D004AEC24' 
ORDER BY timestamp DESC 
LIMIT 5;

-- Kết quả mong đợi:
-- mac: A48D004AEC24
-- employee_id: REVxx ⭐ (đã được map!)
-- employee_name: Nguyễn Văn Test ⭐ (đã được map!)
-- battery: 100.0
-- lat: 10.762400
-- lon: 106.660050
-- timestamp: 2025-11-23 10:30:00
```

### Bước 6: Kiểm tra Hiển thị trên Bản đồ
1. Mở trang `location.html`
2. Kiểm tra:
   - ✅ Marker xuất hiện tại vị trí (10.762400, 106.660050)
   - ✅ Tên hiển thị: **"Nguyễn Văn Test"**
   - ✅ Click vào marker → Popup hiện:
     ```
     Nguyễn Văn Test
     Helmet: A48D004AEC24
     Pin: 100%
     Vị trí: Công nhân
     ```
   - ✅ Trong danh sách bên trái hiện công nhân với trạng thái ACTIVE (màu xanh)

### Bước 7: Kiểm tra Realtime Update
1. Gửi thêm data MQTT với tọa độ khác:
   ```json
   {
     "mac": "A48D004AEC24",
     "battery": 95.0,
     "lat": 10.762600,
     "lon": 106.660250,
     "timestamp": "2025-11-23T10:35:00"
   }
   ```
2. Quan sát bản đồ:
   - Marker di chuyển sang vị trí mới
   - Pin cập nhật thành 95%
   - Tên vẫn là "Nguyễn Văn Test"

## 🐛 Troubleshooting

### Vấn đề 1: Không thấy tên công nhân trên bản đồ
**Nguyên nhân:** MAC address không khớp

**Giải pháp:**
```sql
-- Kiểm tra MAC trong employees
SELECT employee_id, name, mac_address FROM employees;

-- Kiểm tra MAC trong helmet_data
SELECT DISTINCT mac FROM helmet_data;

-- Đảm bảo MAC khớp chính xác (case-sensitive)
UPDATE employees SET mac_address = 'A48D004AEC24' 
WHERE employee_id = 'REVxx';
```

### Vấn đề 2: employee_id và employee_name NULL trong helmet_data
**Nguyên nhân:** MQTT data đến trước khi gán MAC cho employee

**Giải pháp:**
1. Xóa dữ liệu cũ:
   ```sql
   DELETE FROM helmet_data WHERE mac = 'A48D004AEC24';
   ```
2. Gửi lại MQTT data mới

### Vấn đề 3: Không hiện trên bản đồ dù có dữ liệu
**Nguyên nhân:** Tọa độ (0.0, 0.0) hoặc thiếu dữ liệu helmet_data

**Giải pháp:**
```sql
-- Kiểm tra dữ liệu mới nhất
SELECT mac, employee_id, employee_name, lat, lon, battery, timestamp
FROM helmet_data
WHERE mac = 'A48D004AEC24'
ORDER BY timestamp DESC
LIMIT 1;

-- Đảm bảo lat, lon không phải (0.0, 0.0)
-- Đảm bảo timestamp gần với thời gian hiện tại
```

## 📊 Monitoring Queries

### Query 1: Danh sách công nhân và MAC
```sql
SELECT 
    w.employee_id,
    w.full_name,
    e.mac_address,
    h.helmet_id,
    h.battery_level,
    h.last_seen
FROM workers w
LEFT JOIN employees e ON w.employee_id = e.employee_id
LEFT JOIN helmets h ON h.mac_address = e.mac_address
ORDER BY w.created_at DESC;
```

### Query 2: Dữ liệu MQTT mới nhất từng mũ
```sql
SELECT 
    hd.mac,
    hd.employee_id,
    hd.employee_name,
    hd.battery,
    hd.lat,
    hd.lon,
    hd.timestamp,
    TIMESTAMPDIFF(SECOND, hd.timestamp, NOW()) AS seconds_ago
FROM helmet_data hd
INNER JOIN (
    SELECT mac, MAX(timestamp) AS max_ts
    FROM helmet_data
    GROUP BY mac
) latest ON hd.mac = latest.mac AND hd.timestamp = latest.max_ts
ORDER BY hd.timestamp DESC;
```

### Query 3: Kiểm tra sync giữa workers và employees
```sql
SELECT 
    w.employee_id,
    w.full_name AS worker_name,
    e.name AS employee_name,
    e.mac_address,
    CASE 
        WHEN e.employee_id IS NULL THEN '❌ Not synced'
        WHEN e.mac_address IS NULL THEN '⚠️ No MAC'
        ELSE '✅ Synced'
    END AS sync_status
FROM workers w
LEFT JOIN employees e ON w.employee_id = e.employee_id
ORDER BY w.created_at DESC;
```

## 🎯 Checklist Hoàn Thành

- [ ] Thêm công nhân thành công trong `employees.html`
- [ ] Gán MAC address cho công nhân
- [ ] Kiểm tra bảng `workers` có dữ liệu đúng
- [ ] Kiểm tra bảng `employees` có MAC address đúng
- [ ] Kiểm tra bảng `helmets` có worker_id đúng
- [ ] Gửi MQTT data từ mũ
- [ ] Kiểm tra backend log nhận được MQTT
- [ ] Kiểm tra `helmet_data` có employee_id và employee_name
- [ ] Mở `location.html` thấy marker trên bản đồ
- [ ] Click marker thấy tên công nhân đúng
- [ ] Gửi MQTT mới thấy marker di chuyển realtime

## 📝 Notes

1. **MAC Address Format:** Phải là 12 ký tự hex uppercase (VD: `A48D004AEC24`)
2. **Timestamp:** Phải gần với thời gian hiện tại (< 20 giây thì hiện ACTIVE)
3. **GPS Coordinates:** Phải khác (0.0, 0.0) để hiện trên bản đồ
4. **Employee Sync:** Tự động khi tạo/cập nhật worker qua `employees.html`
5. **Realtime Update:** WebSocket tự động push khi có MQTT data mới

---
✅ **Hệ thống đang hoạt động chính xác!** 
- Backend đã sync đúng giữa `workers` và `employees`
- MQTT handler đã map đúng MAC → employee_id & employee_name
- Map đã hiển thị đúng dữ liệu từ `employees` + `helmet_data`
