# Quick Start - Test MQTT Integration Locally

## 🚀 Chạy Backend Local

```bash
# Build project
mvn clean package -DskipTests

# Chạy Spring Boot
mvn spring-boot:run
```

Backend sẽ chạy tại: http://localhost:8080

## 📝 Tạo nhân viên và gán MAC

1. Mở: http://localhost:8080/manage-employees.html
2. Thêm nhân viên:
   - Mã NV: **NV001**
   - Họ tên: **Nguyễn Văn An**
   - MAC Address: **A48D004AEC24**
3. Click "Thêm nhân viên"

## 🧪 Test MQTT

### Cài MQTT Client
```bash
npm install -g mqtt
```

### Gửi test message (PIN YẾU - sẽ trigger alert)
```bash
mqtt publish -h d0a82f39864c4e86a0551feaed97f7c5.s1.eu.hivemq.cloud -p 8883 -u truong123 -P Truong123 --protocol mqtts -t "helmet/A48D004AEC24" -m "{\"mac\":\"A48D004AEC24\",\"voltage\":11.58,\"current\":-33.3,\"power\":390.0,\"battery\":15.0,\"lat\":10.762400,\"lon\":106.660050,\"counter\":1,\"timestamp\":\"2025-11-10T01:00:00\"}"
```

Battery = 15% < 20% → Backend sẽ gửi cảnh báo qua Messenger!

### Gửi test message (BÌNH THƯỜNG)
```bash
mqtt publish -h d0a82f39864c4e86a0551feaed97f7c5.s1.eu.hivemq.cloud -p 8883 -u truong123 -P Truong123 --protocol mqtts -t "helmet/A48D004AEC24" -m "{\"mac\":\"A48D004AEC24\",\"voltage\":11.58,\"current\":-33.3,\"power\":390.0,\"battery\":100.0,\"lat\":10.762400,\"lon\":106.660050,\"counter\":2,\"timestamp\":\"2025-11-10T02:00:00\"}"
```

Battery = 100% → Không có cảnh báo, chỉ lưu dữ liệu.

## 📊 Kiểm tra logs

```bash
# Terminal chạy Spring Boot sẽ hiển thị:
📩 Received MQTT message from topic: helmet/A48D004AEC24
📦 Payload: {"mac":"A48D004AEC24",...}
👤 Mapped MAC A48D004AEC24 to Employee: Nguyễn Văn An (NV001)
✅ Saved helmet data: MAC=A48D004AEC24, Battery=15.0%, Voltage=11.58V
🚨 Danger alert broadcasted for MAC: A48D004AEC24
```

## 🎯 Next Steps

1. ✅ Test local xong → Deploy lên Heroku
2. ✅ Chạy Raspberry Pi Gateway với code Python
3. ✅ Kết nối ESP32 helmet với Gateway
4. ✅ Nhận cảnh báo real-time qua Messenger!

---

**Full docs**: Xem file `MQTT_INTEGRATION_GUIDE.md`
