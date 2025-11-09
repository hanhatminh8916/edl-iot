# 🗺️ Test Bản Đồ Realtime - Đà Nẵng

## 📍 Tọa độ thực tế từ MQTT
- **Lat**: 15.97331
- **Lon**: 108.25183  
- **MAC**: A48D004AEC24
- **Địa điểm**: Đà Nẵng, Việt Nam

---

## ⚡ Quick Test - 3 bước

### **Bước 1: Chạy Backend**
```bash
mvn spring-boot:run
```

### **Bước 2: Tạo nhân viên NV001**
1. Mở: http://localhost:8080/manage-employees.html
2. Thêm:
   - Mã NV: **NV001**
   - Họ tên: **Nguyễn Văn An** 
   - MAC: **A48D004AEC24**
3. Click "Thêm nhân viên"

### **Bước 3: Gửi dữ liệu MQTT**
```bash
mqtt publish -h d0a82f39864c4e86a0551feaed97f7c5.s1.eu.hivemq.cloud -p 8883 -u truong123 -P Truong123 --protocol mqtts -t "helmet/A48D004AEC24" -m "{\"mac\":\"A48D004AEC24\",\"voltage\":11.51,\"current\":-35.6,\"power\":410.0,\"battery\":100.0,\"lat\":15.97331,\"lon\":108.25183,\"counter\":1409,\"timestamp\":\"2025-11-10T01:27:41.987164\"}"
```

---

## 🎯 Kết quả mong đợi

### **1. Backend logs:**
```
📩 Received MQTT message from topic: helmet/A48D004AEC24
📦 Payload: {"mac":"A48D004AEC24","voltage":11.51,...}
👤 Mapped MAC A48D004AEC24 to Employee: Nguyễn Văn An (NV001)
✅ Saved helmet data: MAC=A48D004AEC24, Battery=100.0%, Voltage=11.51V
```

### **2. Bản đồ:**
1. Mở: http://localhost:8080/location.html
2. Bản đồ sẽ tự động zoom về **Đà Nẵng**
3. Marker **xanh lá** xuất hiện tại tọa độ **15.97331, 108.25183**
4. Click marker → Popup hiển thị:
   - Tên: **Nguyễn Văn An**
   - MAC: **A48D004AEC24**
   - Pin: **100%**
   - Status: **An toàn** (trong vòng tròn an toàn)

### **3. Danh sách bên phải:**
```
👤 Nguyễn Văn An
   ID: NV001
   🟢 An toàn
   🔋 Pin: 100%
```

---

## 🧪 Test các trường hợp khác

### **Test 1: Pin yếu (ALERT - Màu cam/đỏ)**
```bash
mqtt publish -h d0a82f39864c4e86a0551feaed97f7c5.s1.eu.hivemq.cloud -p 8883 -u truong123 -P Truong123 --protocol mqtts -t "helmet/A48D004AEC24" -m "{\"mac\":\"A48D004AEC24\",\"voltage\":11.51,\"current\":-35.6,\"power\":410.0,\"battery\":15.0,\"lat\":15.97331,\"lon\":108.25183,\"counter\":1410,\"timestamp\":\"2025-11-10T01:30:00\"}"
```
→ Marker chuyển màu **đỏ** (ALERT)  
→ Gửi cảnh báo qua Messenger  

### **Test 2: Di chuyển (cập nhật vị trí)**
```bash
mqtt publish -h d0a82f39864c4e86a0551feaed97f7c5.s1.eu.hivemq.cloud -p 8883 -u truong123 -P Truong123 --protocol mqtts -t "helmet/A48D004AEC24" -m "{\"mac\":\"A48D004AEC24\",\"voltage\":11.51,\"current\":-35.6,\"power\":410.0,\"battery\":100.0,\"lat\":15.97350,\"lon\":108.25200,\"counter\":1411,\"timestamp\":\"2025-11-10T01:35:00\"}"
```
→ Marker **di chuyển** đến vị trí mới  
→ Bản đồ tự động refresh sau 10 giây  

### **Test 3: Ngoài vòng tròn (>200m)**
```bash
mqtt publish -h d0a82f39864c4e86a0551feaed97f7c5.s1.eu.hivemq.cloud -p 8883 -u truong123 -P Truong123 --protocol mqtts -t "helmet/A48D004AEC24" -m "{\"mac\":\"A48D004AEC24\",\"voltage\":11.51,\"current\":-35.6,\"power\":410.0,\"battery\":100.0,\"lat\":15.97550,\"lon\":108.25450,\"counter\":1412,\"timestamp\":\"2025-11-10T01:40:00\"}"
```
→ Marker **đỏ** (ngoài khu vực an toàn)  
→ Popup hiển thị: "Ngoài khu vực (XXXm)"  

---

## 🎨 Màu sắc marker

| Trạng thái | Màu | Điều kiện |
|-----------|-----|-----------|
| 🟢 An toàn | Xanh lá | Trong vòng 0-160m (0-80%) + battery OK |
| 🟠 Cảnh báo | Cam | Trong vòng 160-200m (80-100%) |
| 🔴 Nguy hiểm | Đỏ | Ngoài vòng >200m HOẶC battery<20% HOẶC voltage<10V |
| ⚫ Offline | Xám | Không cập nhật >5 phút |

---

## 🔄 Auto refresh

Bản đồ tự động reload data mỗi **10 giây** từ API:
```
GET /api/dashboard/map-data
```

Bạn có thể gửi nhiều message MQTT liên tục, bản đồ sẽ cập nhật tự động!

---

## 📊 REST API Response

```json
[
  {
    "id": "NV001",
    "name": "Nguyễn Văn An",
    "position": "Công nhân",
    "department": "Sản xuất",
    "helmet": {
      "helmetId": "A48D004AEC24",
      "status": "ACTIVE",
      "batteryLevel": 100,
      "lastLocation": {
        "latitude": 15.97331,
        "longitude": 108.25183
      }
    }
  }
]
```

---

## 🚀 Deploy lên Heroku

```bash
git add .
git commit -m "Add real-time map with Da Nang coordinates from MQTT"
git push heroku main
```

Sau khi deploy: https://edl-safework-iot-bf3ee691c9f6.herokuapp.com/location.html

---

## 🎯 Next: Thêm nhiều helmet

1. Tạo thêm NV002, NV003... với MAC khác nhau
2. Gửi MQTT từ nhiều helmet cùng lúc
3. Bản đồ hiển thị tất cả workers realtime!

**Chúc bạn thành công! 🗺️**
