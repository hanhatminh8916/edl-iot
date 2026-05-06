# 🪖 AIoT Smart Helmet Backend

<!-- EN -->
> Backend system for the **AIoT Smart Helmet** solution — real-time worker monitoring in hazardous industrial environments (power, construction, mining) via IoT sensors, GPS/UWB positioning, and emergency alerts over MQTT.

<!-- VN -->
> Hệ thống backend cho giải pháp **Mũ bảo hộ thông minh AIoT** — giám sát thời gian thực công nhân trong môi trường công nghiệp nguy hiểm (điện lực, xây dựng, hầm mỏ) thông qua cảm biến IoT, định vị GPS/UWB và cảnh báo khẩn cấp qua MQTT.

---

## 📋 Table of Contents / Mục lục

| EN | VN |
|---|---|
| [Overview](#overview) | [Tổng quan](#overview) |
| [System Architecture](#system-architecture) | [Kiến trúc hệ thống](#system-architecture) |
| [Key Features](#key-features) | [Tính năng chính](#key-features) |
| [Tech Stack](#tech-stack) | [Công nghệ sử dụng](#tech-stack) |
| [Project Structure](#project-structure) | [Cấu trúc dự án](#project-structure) |
| [Requirements](#requirements) | [Yêu cầu hệ thống](#requirements) |
| [Setup & Run](#setup--run) | [Cài đặt & Chạy](#setup--run) |
| [MQTT Configuration (HiveMQ)](#mqtt-configuration-hivemq) | [Cấu hình MQTT (HiveMQ)](#mqtt-configuration-hivemq) |
| [API Endpoints](#api-endpoints) | [API Endpoints](#api-endpoints) |
| [WebSocket & Real-time](#websocket--real-time) | [WebSocket & Real-time](#websocket--real-time) |
| [Alert & Notification Flow](#alert--notification-flow) | [Cảnh báo & Thông báo](#alert--notification-flow) |
| [Deployment (Heroku)](#deployment-heroku) | [Triển khai (Heroku)](#deployment-heroku) |
| [Related Documents](#related-documents) | [Tài liệu liên quan](#related-documents) |

---

## Overview / Tổng quan

<!-- EN -->
The system ingests data from **smart helmets** (ESP32 + sensors) transmitted via **LoRa Gateway** → **MQTT Broker (HiveMQ Cloud)**. The backend processes in real-time:

- 📡 **Telemetry**: battery, voltage, current, temperature, heart rate, SpO2
- 📍 **Positioning**: outdoor GPS + indoor UWB (2D Positioning)
- 🚨 **Alerts**: fall detection (FALL), emergency help (SOS), danger zones, low battery
- 📲 **Notifications**: Facebook Messenger, Web Push, WebSocket real-time
- 🤖 **AI/LLM**: data analytics, voice assistant

<!-- VN -->
Hệ thống nhận dữ liệu từ các **mũ bảo hộ thông minh** (ESP32 + cảm biến) truyền qua **LoRa Gateway** → **MQTT Broker (HiveMQ Cloud)**. Backend xử lý real-time:

- 📡 **Telemetry**: pin, điện áp, dòng điện, nhiệt độ, nhịp tim, SpO2
- 📍 **Định vị**: GPS ngoài trời + UWB trong nhà (2D Positioning)
- 🚨 **Cảnh báo**: phát hiện ngã (FALL), yêu cầu trợ giúp (SOS), khu vực nguy hiểm, pin yếu
- 📲 **Thông báo**: Facebook Messenger, Web Push, WebSocket real-time
- 🤖 **AI/LLM**: phân tích dữ liệu, trợ lý giọng nói

---

## System Architecture / Kiến trúc hệ thống

```
┌─────────────┐     LoRa      ┌──────────────┐     MQTT      ┌──────────────────┐
│ Smart Helmet│──────────────▶│ LoRa Gateway  │──────────────▶│  HiveMQ Cloud    │
│  (ESP32)    │               │  (Python)     │               │  (MQTT Broker)   │
└─────────────┘               └──────────────┘               └────────┬─────────┘
                                                                      │
                                                              MQTT Subscribe
                                                                      │
                                                              ┌───────▼─────────┐
                                                              │   Spring Boot   │
                                                              │   Backend       │
                                                              │                 │
                                                              │ ┌─────────────┐ │
                                                              │ │ MQTT Handler│ │
                                                              │ └──────┬──────┘ │
                                                              │        │        │
                                                              │ ┌──────▼──────┐ │
                                                              │ │  Alert      │ │
                                                              │ │  Engine     │ │
                                                              │ └──────┬──────┘ │
                                                              │        │        │
                                                              │ ┌──────▼──────┐ │
                                                              │ │  WebSocket  │ │
                                                              │ │  Redis Pub  │ │
                                                              │ │  Messenger  │ │
                                                              │ │  Web Push   │ │
                                                              │ └─────────────┘ │
                                                              └─────────────────┘
                                                                      │
                                                           ┌──────────┼──────────┐
                                                           │          │          │
                                                      ┌────▼───┐ ┌───▼────┐ ┌───▼────┐
                                                      │ Web UI │ │Mobile  │ │Messenger│
                                                      │(Dashboard)│ │(PWA)  │ │(FB)    │
                                                      └────────┘ └────────┘ └────────┘
```

---

## Key Features / Tính năng chính

### 📡 Real-time Monitoring / Giám sát thời gian thực

<!-- EN -->
- Ingests MQTT data from smart helmets via HiveMQ Cloud
- Telemetry: battery, voltage, current, power
- Health: temperature, heart rate, SpO2
- GPS positioning (lat/lon) with cache fallback on signal loss
- LoRa signal quality: RSSI, SNR

<!-- VN -->
- Nhận dữ liệu MQTT từ mũ bảo hộ qua HiveMQ Cloud
- Telemetry: pin, điện áp, dòng điện, công suất
- Sức khỏe: nhiệt độ, nhịp tim, SpO2
- Định vị GPS (lat/lon) với fallback cache khi mất tín hiệu
- Chất lượng tín hiệu LoRa: RSSI, SNR

### 🚨 Smart Alerts / Cảnh báo thông minh

| Alert Type / Loại | Description / Mô tả | Severity / Mức độ |
|---|---|---|
| **FALL** | Fall detected via accelerometer / Phát hiện ngã từ cảm biến gia tốc | 🔴 CRITICAL |
| **HELP_REQUEST** | Worker pressed SOS button / Công nhân nhấn nút SOS khẩn cấp | 🔴 CRITICAL |
| **DANGER_ZONE** | Entered danger zone (UWB Anchor) / Vào khu vực nguy hiểm | 🟠 WARNING |
| **LOW_BATTERY** | Battery below 20% / Pin dưới 20% | 🟡 WARNING |
| **LOW_VOLTAGE** | Voltage below 10V / Điện áp dưới 10V | 🟡 WARNING |
| **HIGH_CURRENT** | Abnormal current > 50A / Dòng điện bất thường > 50A | 🟡 WARNING |

<!-- EN -->
- **Debounce**: anti-spam for alerts (30-60 seconds)
- **MAC Noise Filter**: filters unknown device noise (requires ≥ 9 messages for confirmation)
- **UPSERT Alert**: each helmet has only 1 alert per type, auto-resolves when signal returns to 0

<!-- VN -->
- **Debounce**: chống spam cảnh báo (30-60 giây)
- **MAC Noise Filter**: lọc nhiễu thiết bị lạ (cần ≥ 9 messages để xác nhận)
- **UPSERT Alert**: mỗi helmet chỉ có 1 alert mỗi loại, tự động resolve khi tín hiệu về 0

### 📍 Positioning & Mapping / Định vị & Bản đồ

<!-- EN -->
- **GPS**: outdoor positioning, caches last known position on signal loss
- **UWB 2D Positioning**: indoor positioning with 4 anchors (A0-A3)
- **SafeZone**: safe/danger zone management
- Realtime map dashboard powered by Leaflet.js

<!-- VN -->
- **GPS**: định vị ngoài trời, cache vị trí cũ khi mất tín hiệu
- **UWB 2D Positioning**: định vị trong nhà với 4 anchors (A0-A3)
- **SafeZone**: quản lý khu vực an toàn/nguy hiểm
- Realtime map dashboard với Leaflet.js

### 📲 Multi-channel Notifications / Thông báo đa kênh

<!-- EN -->
- **WebSocket (STOMP)**: real-time dashboard, location updates, alerts
- **Redis Pub/Sub**: horizontal scaling across multiple instances
- **Facebook Messenger**: CRITICAL alerts via Fanpage
- **Web Push**: browser push notifications (PWA)

<!-- VN -->
- **WebSocket (STOMP)**: real-time dashboard, cập nhật vị trí, cảnh báo
- **Redis Pub/Sub**: scale ngang nhiều instance
- **Facebook Messenger**: gửi cảnh báo CRITICAL qua Fanpage
- **Web Push**: push notification trên trình duyệt (PWA)

### 🤖 AI & Assistant / AI & Trợ lý

<!-- EN -->
- **LLM Analytics**: AI-powered sensor data analysis
- **Voice Assistant**: Vietnamese voice assistant
- **ADK Integration**: Agent Development Kit integration

<!-- VN -->
- **LLM Analytics**: phân tích dữ liệu cảm biến bằng AI
- **Voice Assistant**: trợ lý giọng nói tiếng Việt
- **ADK Integration**: tích hợp Agent Development Kit

### 🛡️ Management / Quản lý

<!-- EN -->
- Employee management — assign helmets to workers
- Helmet management — track by MAC address
- Anchor management (UWB) — zone configuration
- Statistics dashboard — system overview
- Query Monitor — database performance monitoring

<!-- VN -->
- Quản lý công nhân (Employee) — gán mũ bảo hộ
- Quản lý mũ bảo hộ (Helmet) — theo dõi MAC address
- Quản lý Anchor (UWB) — cấu hình khu vực
- Dashboard thống kê — tổng quan hệ thống
- Query Monitor — giám sát hiệu năng database

---

## Tech Stack / Công nghệ sử dụng

| Component / Thành phần | Technology / Công nghệ | Version / Phiên bản |
|---|---|---|
| **Language / Ngôn ngữ** | Java | 17 |
| **Framework** | Spring Boot | 3.5.6 |
| **Build Tool** | Maven | 3.9.6 |
| **Database / Cơ sở dữ liệu** | MySQL (JawsDB) / H2 (test) | — |
| **Cache** | Redis + Caffeine | — |
| **MQTT Client** | Eclipse Paho + Spring Integration MQTT | 1.2.5 |
| **MQTT Broker** | HiveMQ Cloud | — |
| **WebSocket** | Spring WebSocket + STOMP | — |
| **Messaging** | Facebook Messenger API (v21.0) | — |
| **Web Push** | Web-Push + BouncyCastle | 5.1.1 |
| **HTTP Client** | Spring WebFlux (WebClient) | — |
| **Container** | Docker Compose (Redis local) | — |
| **Deploy** | Heroku | — |

---

## Project Structure / Cấu trúc dự án

```
edl-iot/
├── src/main/java/com/hatrustsoft/bfe_foraiot/
│   ├── BfeForAiotApplication.java      # Entry point
│   ├── config/                          # Configuration (MQTT, WebSocket, Redis, Cache)
│   ├── controller/                      # REST API Controllers (20 controllers)
│   ├── dto/                             # Data Transfer Objects
│   ├── entity/                          # JPA Entities (7 entities)
│   ├── model/                           # Domain Models (Alert, Helmet, SafeZone...)
│   ├── publisher/                       # Redis Publisher
│   ├── repository/                      # Spring Data JPA Repositories
│   ├── scheduler/                       # Scheduled Tasks
│   ├── service/                         # Business Logic (18 services)
│   └── util/                            # Utilities (VietnamTimeUtils...)
├── src/main/resources/
│   ├── application-h2.properties        # H2 config (test)
│   ├── application-heroku.properties    # Heroku production config
│   ├── schema.sql                       # Database schema
│   └── static/                          # Frontend (Dashboard, Map, PWA)
├── docker-compose.yml                   # Redis local development
├── Procfile                             # Heroku deployment
├── system.properties                    # Java + Maven version
├── pom.xml                              # Maven dependencies
└── simulate-moving-tags.ps1             # PowerShell simulation script
```

---

## Requirements / Yêu cầu hệ thống

<!-- EN -->
- **JDK 17** or higher
- **Maven 3.9+**
- **MySQL 8.0** (or H2 for development)
- **Redis** (can use Docker)
- **HiveMQ Cloud** account (MQTT Broker)

<!-- VN -->
- **JDK 17** trở lên
- **Maven 3.9+**
- **MySQL 8.0** (hoặc H2 cho development)
- **Redis** (có thể dùng Docker)
- **HiveMQ Cloud** account (MQTT Broker)

---

## Setup & Run / Cài đặt & Chạy

### 1. Clone repository

```bash
git clone https://github.com/hanhatminh8916/AIoT-Smart-Helmet-Backend.git
cd AIoT-Smart-Helmet-Backend
```

### 2. Start Redis (Docker) / Khởi động Redis

```bash
docker-compose up -d
```

### 3. Configure environment variables / Cấu hình biến môi trường

<!-- EN -->
Create a `.env` file or set environment variables:

<!-- VN -->
Tạo file `.env` hoặc set environment variables:

```bash
# MQTT (HiveMQ Cloud)
export MQTT_BROKER_URL=ssl://your-cluster.hivemq.cloud:8883
export MQTT_USERNAME=your_username
export MQTT_PASSWORD=your_password
export MQTT_TOPIC=helmet/#

# Database / Cơ sở dữ liệu (MySQL)
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/aiot_helmet
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD=your_password

# Facebook Messenger (optional / tùy chọn)
export FACEBOOK_PAGE_ACCESS_TOKEN=your_page_token
export FACEBOOK_VERIFY_TOKEN=your_verify_token

# Redis
export SPRING_DATA_REDIS_HOST=localhost
export SPRING_DATA_REDIS_PORT=6379
```

### 4. Run the application / Chạy ứng dụng

```bash
# Development (H2 database)
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2

# Production (MySQL)
./mvnw spring-boot:run
```

<!-- EN -->
Application runs at: **http://localhost:8080**

<!-- VN -->
Ứng dụng chạy tại: **http://localhost:8080**

### 5. Build JAR

```bash
./mvnw clean package -DskipTests
java -jar target/BFE_forAIOT-0.0.1-SNAPSHOT.jar
```

---

## MQTT Configuration (HiveMQ) / Cấu hình MQTT (HiveMQ)

<!-- EN -->
The system uses **HiveMQ Cloud** as the MQTT Broker. Configuration in `application-heroku.properties`:

<!-- VN -->
Hệ thống sử dụng **HiveMQ Cloud** làm MQTT Broker. Cấu hình trong `application-heroku.properties`:

```properties
mqtt.broker.url=${MQTT_BROKER_URL}        # ssl://xxx.hivemq.cloud:8883
mqtt.client.id=bfe-backend-${random.value}
mqtt.username=${MQTT_USERNAME}
mqtt.password=${MQTT_PASSWORD}
mqtt.topic=${MQTT_TOPIC}                  # helmet/#
```

### MQTT Payload Format (from ESP32/Gateway) / Định dạng MQTT Payload

```json
{
  "mac": "AA:BB:CC:DD:EE:FF",
  "voltage": 12.5,
  "current": 2.1,
  "power": 26.25,
  "battery": 85.0,
  "lat": 10.762622,
  "lon": 106.660172,
  "temp": 36.5,
  "hr": 72,
  "spo2": 98,
  "counter": 1234,
  "fallDetected": 0,
  "helpRequest": 0,
  "mode": "direct",
  "inDangerZone": false,
  "rssi": -85,
  "snr": 9.5,
  "gateway": "GW_MAC_ADDRESS",
  "uwb": {
    "A0": 3.5,
    "A1": 4.2,
    "A2": 5.1,
    "A3": 2.8
  },
  "timestamp": "2025-05-06T11:25:00"
}
```

---

## API Endpoints

### 🏥 Health Check
| Method | Endpoint | Description / Mô tả |
|---|---|---|
| GET | `/api/health` | System health check / Kiểm tra trạng thái |

### 👷 Employee Management / Quản lý công nhân
| Method | Endpoint | Description / Mô tả |
|---|---|---|
| GET | `/api/employees` | List employees / Danh sách công nhân |
| POST | `/api/employees` | Add employee / Thêm công nhân |
| PUT | `/api/employees/{id}` | Update employee / Cập nhật công nhân |
| DELETE | `/api/employees/{id}` | Delete employee / Xóa công nhân |
| POST | `/api/employees/{id}/assign-helmet` | Assign helmet / Gán mũ bảo hộ |

### ⛑️ Helmet Management / Quản lý mũ bảo hộ
| Method | Endpoint | Description / Mô tả |
|---|---|---|
| GET | `/api/helmets` | List helmets / Danh sách mũ |
| POST | `/api/helmets` | Register new helmet / Đăng ký mũ mới |
| GET | `/api/helmets/{mac}` | Helmet details / Chi tiết mũ |
| GET | `/api/helmets/{mac}/data` | Telemetry data / Dữ liệu telemetry |

### 🚨 Alert Management / Quản lý cảnh báo
| Method | Endpoint | Description / Mô tả |
|---|---|---|
| GET | `/api/alerts` | List alerts / Danh sách cảnh báo |
| GET | `/api/alerts/active` | Active alerts / Cảnh báo đang hoạt động |
| PUT | `/api/alerts/{id}/acknowledge` | Acknowledge alert / Xác nhận cảnh báo |
| PUT | `/api/alerts/{id}/resolve` | Resolve alert / Xử lý cảnh báo |

### 📍 Location & Positioning / Định vị
| Method | Endpoint | Description / Mô tả |
|---|---|---|
| GET | `/api/locations` | All helmet positions / Vị trí tất cả mũ |
| GET | `/api/locations/{mac}` | Single helmet position / Vị trí một mũ |
| GET | `/api/positioning/anchors` | UWB anchor list / Danh sách UWB anchors |
| GET | `/api/positioning/realtime` | UWB real-time positions / Vị trí UWB real-time |

### 🗺️ SafeZone Management / Quản lý khu vực an toàn
| Method | Endpoint | Description / Mô tả |
|---|---|---|
| GET | `/api/safezones` | List safe zones / Danh sách khu vực |
| POST | `/api/safezones` | Create safe zone / Tạo khu vực |
| PUT | `/api/safezones/{id}` | Update safe zone / Cập nhật khu vực |
| DELETE | `/api/safezones/{id}` | Delete safe zone / Xóa khu vực |

### 📊 Dashboard
| Method | Endpoint | Description / Mô tả |
|---|---|---|
| GET | `/api/dashboard/summary` | System overview / Tổng quan hệ thống |
| GET | `/api/dashboard/stats` | Detailed statistics / Thống kê chi tiết |

### 🤖 AI & Voice / AI & Giọng nói
| Method | Endpoint | Description / Mô tả |
|---|---|---|
| POST | `/api/llm/analyze` | AI data analysis / Phân tích dữ liệu bằng AI |
| POST | `/api/voice/command` | Voice command processing / Xử lý lệnh giọng nói |

### 📲 Messenger Webhook
| Method | Endpoint | Description / Mô tả |
|---|---|---|
| GET | `/webhook/messenger` | Verify webhook / Xác thực webhook |
| POST | `/webhook/messenger` | Receive messages / Nhận tin nhắn |

### 🔔 Push Notification / Thông báo đẩy
| Method | Endpoint | Description / Mô tả |
|---|---|---|
| POST | `/api/push/subscribe` | Subscribe to push / Đăng ký push |
| POST | `/api/push/unsubscribe` | Unsubscribe from push / Hủy đăng ký |

> 📖 See details at / Xem chi tiết tại: [`API_DOCUMENTATION.md`](API_DOCUMENTATION.md) & [`API-Integration-Guide.md`](API-Integration-Guide.md)

---

## WebSocket & Real-time

<!-- EN -->
The system uses **STOMP over WebSocket** for real-time communication.

<!-- VN -->
Hệ thống sử dụng **STOMP over WebSocket** cho real-time communication.

### Connection / Kết nối

```javascript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);
stompClient.connect({}, () => {
  // Subscribe to topics / Subscribe các topic
});
```

### Topics

| Topic | Description / Mô tả |
|---|---|
| `/topic/helmets/realtime` | Real-time telemetry / Dữ liệu telemetry real-time |
| `/topic/helmets/positions` | Real-time GPS positions / Vị trí GPS real-time |
| `/topic/positioning/uwb` | Real-time UWB 2D positions / Vị trí UWB 2D real-time |
| `/topic/alerts/new` | New alerts / Cảnh báo mới |
| `/topic/alerts/update` | Alert status updates / Cập nhật trạng thái cảnh báo |
| `/topic/safezones` | Safe zone updates / Cập nhật khu vực an toàn |

---

## Alert & Notification Flow / Cảnh báo & Thông báo

### Alert Processing Flow / Luồng xử lý cảnh báo

```
MQTT Message → MqttMessageHandler → Alert Engine
    │
    ├── fallDetected=1 → createFallDetectedAlert()
    │       ├── UPSERT Alert (FALL, CRITICAL, PENDING)
    │       ├── WebSocket: /topic/alerts/new
    │       ├── Messenger: broadcastDangerAlert()
    │       └── Web Push: sendAlertPush()
    │
    ├── helpRequest=1 → createHelpRequestAlert()
    │       └── (same as above / tương tự như trên)
    │
    ├── inDangerZone=true → checkDangerZoneAlert()
    │       └── Log warning (no Messenger / không gửi Messenger)
    │
    └── fallDetected=0 / helpRequest=0 → resolveAlert()
            ├── Set status = RESOLVED
            ├── WebSocket: /topic/alerts/update
            └── Messenger: broadcastAlertResolved()
```

### Messenger Configuration / Cấu hình Messenger

<!-- EN -->
See detailed guides at:

<!-- VN -->
Xem hướng dẫn chi tiết tại:

- [`MESSENGER_SETUP_GUIDE.md`](MESSENGER_SETUP_GUIDE.md)
- [`MESSENGER_INTEGRATION_SUMMARY.md`](MESSENGER_INTEGRATION_SUMMARY.md)

---

## Deployment (Heroku) / Triển khai (Heroku)

### Heroku Setup / Cấu hình Heroku

```bash
# Create Heroku app / Tạo app Heroku
heroku create aiot-smart-helmet

# Set Config Vars
heroku config:set MQTT_BROKER_URL=ssl://xxx.hivemq.cloud:8883
heroku config:set MQTT_USERNAME=your_username
heroku config:set MQTT_PASSWORD=your_password
heroku config:set MQTT_TOPIC=helmet/#
heroku config:set SPRING_DATASOURCE_URL=jdbc:mysql://...
heroku config:set SPRING_DATASOURCE_USERNAME=...
heroku config:set SPRING_DATASOURCE_PASSWORD=...
heroku config:set FACEBOOK_PAGE_ACCESS_TOKEN=...
heroku config:set FACEBOOK_VERIFY_TOKEN=...
heroku config:set SPRING_PROFILES_ACTIVE=heroku

# Deploy
git push heroku main
```

<!-- EN -->
See also:

<!-- VN -->
Xem thêm:

- [`HEROKU_DEPLOY_GUIDE.md`](HEROKU_DEPLOY_GUIDE.md)
- [`HEROKU_REDIS_SETUP.md`](HEROKU_REDIS_SETUP.md)
- [`JAWSDB_CONNECTION_GUIDE.md`](JAWSDB_CONNECTION_GUIDE.md)
- [`DEPLOY_STATUS.md`](DEPLOY_STATUS.md)

---

## Related Documents / Tài liệu liên quan

| Document / Tài liệu | Description / Mô tả |
|---|---|
| [`API_DOCUMENTATION.md`](API_DOCUMENTATION.md) | Detailed API docs / Tài liệu API chi tiết |
| [`API-Integration-Guide.md`](API-Integration-Guide.md) | API integration guide / Hướng dẫn tích hợp API |
| [`MQTT_INTEGRATION_GUIDE.md`](MQTT_INTEGRATION_GUIDE.md) | MQTT setup guide / Hướng dẫn cấu hình MQTT |
| [`FALL_ALERT_SUMMARY.md`](FALL_ALERT_SUMMARY.md) | Fall alert overview / Tổng quan cảnh báo ngã |
| [`FALL_ALERT_TEST_GUIDE.md`](FALL_ALERT_TEST_GUIDE.md) | Fall alert testing / Hướng dẫn test cảnh báo ngã |
| [`MESSENGER_SETUP_GUIDE.md`](MESSENGER_SETUP_GUIDE.md) | Messenger setup / Cấu hình Messenger |
| [`MESSENGER_INTEGRATION_SUMMARY.md`](MESSENGER_INTEGRATION_SUMMARY.md) | Messenger integration / Tổng quan tích hợp Messenger |
| [`HEROKU_DEPLOY_GUIDE.md`](HEROKU_DEPLOY_GUIDE.md) | Heroku deploy guide / Hướng dẫn deploy Heroku |
| [`HEROKU_REDIS_SETUP.md`](HEROKU_REDIS_SETUP.md) | Redis on Heroku / Cấu hình Redis trên Heroku |
| [`REDIS_WEBSOCKET_GUIDE.md`](REDIS_WEBSOCKET_GUIDE.md) | Redis + WebSocket scaling |
| [`LLM_INTEGRATION_GUIDE.md`](LLM_INTEGRATION_GUIDE.md) | AI/LLM integration / Tích hợp AI/LLM |
| [`VOICE_ASSISTANT_GUIDE.md`](VOICE_ASSISTANT_GUIDE.md) | Voice assistant / Trợ lý giọng nói |
| [`POSITIONING_OPTIMIZATION.md`](POSITIONING_OPTIMIZATION.md) | UWB positioning optimization / Tối ưu định vị UWB |
| [`EMPLOYEE_HELMET_MAPPING_TEST.md`](EMPLOYEE_HELMET_MAPPING_TEST.md) | Employee-helmet mapping test / Test gán công nhân-mũ |
| [`WHITELIST_GUIDE.md`](WHITELIST_GUIDE.md) | Whitelist guide / Hướng dẫn whitelist |
| [`QUICK_START.md`](QUICK_START.md) | Quick start guide / Hướng dẫn bắt đầu nhanh |
| [`DEBUG_HELP_REQUEST.md`](DEBUG_HELP_REQUEST.md) | Debug help request / Debug Help Request |
| [`MAP_TEST_GUIDE.md`](MAP_TEST_GUIDE.md) | Map testing / Test bản đồ |
| [`REDIS_COMMANDS.md`](REDIS_COMMANDS.md) | Redis commands reference / Redis commands tham khảo |
| [`RESTART_BACKEND.md`](RESTART_BACKEND.md) | Backend restart guide / Hướng dẫn restart backend |

---

## 📜 License

<!-- EN -->
Project by **HaTrustSoft** — Vietnam-Korea University of Information and Communication Technology (VKU).

<!-- VN -->
Dự án thuộc về **HaTrustSoft** — Đại học Công nghệ Thông tin và Truyền thông Việt Hàn (VKU).

---

<p align="center">
  <b>🪖 AIoT Smart Helmet — Protecting Workers with Technology / Bảo vệ người lao động bằng công nghệ</b><br>
  <sub>Powered by Spring Boot • HiveMQ • Redis • WebSocket • AI</sub>
</p>
