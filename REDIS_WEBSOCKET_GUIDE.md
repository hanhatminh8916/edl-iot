# Redis + WebSocket Integration Guide

## 🎯 Kiến trúc mới

```
Gateway (LoRa) → MQTT (HiveMQ) → Backend (Spring Boot)
                                      ↓
                                1. Save to Database
                                2. Publish to Redis
                                      ↓
                                Redis Pub/Sub
                                      ↓
                                Redis Subscriber
                                      ↓
                                WebSocket Push
                                      ↓
                            Frontend (Real-time update)
```

## 📦 Cài đặt Redis (Windows)

### Option 1: Docker (Khuyến nghị)
```bash
docker run --name redis -p 6379:6379 -d redis
```

### Option 2: Download binary
1. Download: https://github.com/microsoftarchive/redis/releases
2. Extract và chạy `redis-server.exe`

### Option 3: WSL2
```bash
wsl --install
wsl
sudo apt update
sudo apt install redis-server
redis-server
```

## 🚀 Chạy ứng dụng

### 1. Start Redis
```bash
# Docker
docker start redis

# Hoặc WSL
redis-server

# Hoặc Windows binary
./redis-server.exe
```

### 2. Kiểm tra Redis
```bash
# Docker
docker exec -it redis redis-cli ping
# Response: PONG

# WSL/Binary
redis-cli ping
# Response: PONG
```

### 3. Start Spring Boot
```bash
mvn clean install
mvn spring-boot:run
```

### 4. Mở Dashboard
```
http://localhost:8080/dashboard-realtime.html
```

## 📊 Flow hoạt động

1. **MQTT Message** → `MqttMessageHandler.handleMessage()`
2. **Save to DB** → `helmetDataRepository.save()`
3. **Publish to Redis** → `redisPublisher.publishHelmetData()` (channel: `helmet:data`)
4. **Redis Subscriber** → `RedisMessageSubscriber.onMessage()`
5. **WebSocket Push** → `messagingTemplate.convertAndSend("/topic/helmet/data")`
6. **Frontend Update** → Browser auto-update dashboard

## 🧪 Testing

### Test với MQTT Simulator
```bash
# Install mosquitto client
brew install mosquitto  # macOS
# hoặc
sudo apt install mosquitto-clients  # Linux

# Publish test message
mosquitto_pub \
  -h d0a82f39864c4e86a0551feaed97f7c5.s1.eu.hivemq.cloud \
  -p 8883 \
  -u truong123 \
  -P Truong123 \
  --capath /etc/ssl/certs/ \
  -t "helmet/data" \
  -m '{"mac":"TEST:00:00:00:00:01","battery":85.5,"voltage":12.3,"lat":16.073844,"lon":108.149441,"mode":"direct"}'
```

### Test Redis
```bash
# Connect to Redis CLI
redis-cli

# Monitor messages
SUBSCRIBE helmet:data

# (Trong terminal khác) Publish test
PUBLISH helmet:data '{"mac":"TEST:00:00:00:00:02","battery":75.0}'
```

### Test WebSocket (Browser Console)
```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected:', frame);
    
    stompClient.subscribe('/topic/helmet/data', function(message) {
        console.log('Received:', JSON.parse(message.body));
    });
});
```

## 🔧 Configuration

### Development (Local Redis)
```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=
```

### Production (Heroku Redis)
```bash
# Add Redis addon
heroku addons:create heroku-redis:mini

# Config tự động set:
# REDIS_URL=redis://h:password@host:port
```

Sửa `application.properties`:
```properties
spring.data.redis.host=${REDIS_HOST}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.password=${REDIS_PASSWORD}
```

## 📈 Monitoring

### Redis Stats
```bash
redis-cli
INFO stats
```

### WebSocket Connections
Check Spring Boot logs:
```
📡 Published to Redis: AA:BB:CC:DD:EE:FF
📥 Received from Redis: AA:BB:CC:DD:EE:FF
📤 Pushed to WebSocket: /topic/helmet/data
```

## 🐛 Troubleshooting

### Redis connection refused
```
Error: Could not connect to Redis at 127.0.0.1:6379: Connection refused
```
**Fix:** Start Redis server

### WebSocket 404
```
Error: WebSocket connection to 'ws://localhost:8080/ws' failed
```
**Fix:** Kiểm tra WebSocketConfig đã được load chưa

### No data on dashboard
1. Kiểm tra MQTT connection
2. Kiểm tra Redis connection
3. Kiểm tra WebSocket connection (F12 Console)
4. Kiểm tra logs Backend

## 🎯 Next Steps

- [ ] Add authentication cho WebSocket
- [ ] Add Redis cache cho helmet data
- [ ] Add WebSocket rooms (1 room = 1 công trường)
- [ ] Add Redis TTL cho stale data
- [ ] Deploy to Heroku with Redis addon

## 📚 References

- Redis Pub/Sub: https://redis.io/docs/interact/pubsub/
- Spring WebSocket: https://docs.spring.io/spring-framework/reference/web/websocket.html
- STOMP Protocol: https://stomp.github.io/
