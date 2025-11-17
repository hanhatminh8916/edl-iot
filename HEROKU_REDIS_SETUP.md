# 🚀 Heroku Redis Setup - Step by Step

## Tổng quan
Hướng dẫn cấu hình Redis trên Heroku cho hệ thống BFE_forAIOT (Real-time WebSocket push)

---

## 📋 Yêu cầu
- [x] Đã có app Heroku (bfe-foraiot hoặc tên khác)
- [x] Đã cài Heroku CLI: `heroku --version`
- [x] Đã login: `heroku login`
- [x] Code đã có Redis integration (✅ Done)

---

## 🎯 Bước 1: Thêm Heroku Redis Add-on

### Qua Heroku CLI (Khuyến nghị)
```bash
# Login nếu chưa
heroku login

# Thêm Redis addon (Plan: mini - FREE)
heroku addons:create heroku-redis:mini -a bfe-foraiot

# Output:
# Creating heroku-redis:mini on ⬢ bfe-foraiot... free
# Your add-on should be available in a few seconds.
# redis-whatever-12345 is being created in the background.
```

### Qua Heroku Dashboard (UI)
1. Vào **Heroku Dashboard**: https://dashboard.heroku.com
2. Chọn app **bfe-foraiot**
3. Tab **Resources**
4. Phần **Add-ons** → Click **Find more add-ons**
5. Tìm **Heroku Redis**
6. Chọn plan **Mini** (Free)
7. Click **Submit Order Form**

---

## 🔍 Bước 2: Kiểm tra Redis đã được tạo

```bash
# Xem thông tin Redis
heroku addons:info heroku-redis -a bfe-foraiot

# Output:
# === redis-whatever-12345
# Plan:        heroku-redis:mini
# Price:       free
# State:       created
# Created at:  ...
```

### Kiểm tra biến môi trường (tự động set)
```bash
heroku config -a bfe-foraiot | findstr REDIS

# Output:
# REDIS_TLS_URL: rediss://h:p1234567890abcdef@ec2-xx-xx-xx-xx.compute-1.amazonaws.com:12345
# REDIS_URL:     redis://h:p1234567890abcdef@ec2-xx-xx-xx-xx.compute-1.amazonaws.com:12345
```

**QUAN TRỌNG:**
- Heroku tự động set `REDIS_URL` 
- Format: `redis://:password@host:port`
- Spring Boot tự động parse `REDIS_URL` → không cần config thủ công!

---

## ⚙️ Bước 3: Update application.properties (ĐÃ XONG)

File `src/main/resources/application.properties` **đã có sẵn**:

```properties
# ========================================
# REDIS CONFIGURATION
# ========================================
# Local Redis (Development)
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=
spring.data.redis.timeout=60000

# Redis Cloud (Production - Heroku)
# Uncomment khi deploy lên Heroku
# spring.data.redis.host=${REDIS_HOST}
# spring.data.redis.port=${REDIS_PORT:6379}
# spring.data.redis.password=${REDIS_PASSWORD}
```

### 🎯 Làm gì bây giờ?

**CÁCH 1: Để nguyên (Khuyến nghị - Spring Boot 2.4+)**
- Spring Boot tự động detect `REDIS_URL` từ Heroku
- Không cần uncomment dòng nào
- ✅ Hoạt động cả local (localhost:6379) và Heroku (auto parse REDIS_URL)

**CÁCH 2: Uncomment (Nếu CÁCH 1 không work)**
```properties
# Uncomment 3 dòng này nếu Spring Boot không tự parse REDIS_URL
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.password=${REDIS_PASSWORD:}
```

Sau đó set manual config vars trên Heroku:
```bash
# Parse REDIS_URL thủ công
heroku config:set REDIS_HOST=ec2-xx-xx-xx-xx.compute-1.amazonaws.com -a bfe-foraiot
heroku config:set REDIS_PORT=12345 -a bfe-foraiot
heroku config:set REDIS_PASSWORD=p1234567890abcdef -a bfe-foraiot
```

---

## 📦 Bước 4: Deploy lên Heroku

### Qua Git (CLI)
```bash
# Commit code (nếu có thay đổi)
git add .
git commit -m "Add Redis + WebSocket support"

# Push lên Heroku
git push heroku main

# Hoặc nếu dùng branch khác:
git push heroku your-branch:main
```

### Qua GitHub (Dashboard)
1. Vào **Heroku Dashboard** → App **bfe-foraiot**
2. Tab **Deploy**
3. Phần **Deployment method** → Chọn **GitHub**
4. Connect repository: **BFE_forAIOT**
5. Chọn branch: **main**
6. Click **Deploy Branch**

---

## ✅ Bước 5: Kiểm tra Redis hoạt động

### Test 1: Connect Redis CLI
```bash
# Connect to Heroku Redis
heroku redis:cli -a bfe-foraiot

# Trong Redis CLI:
127.0.0.1:12345> PING
PONG

127.0.0.1:12345> INFO stats
# Hiển thị thống kê Redis

127.0.0.1:12345> SUBSCRIBE helmet:data
Reading messages... (press Ctrl-C to quit)
```

### Test 2: Xem logs backend
```bash
# Xem logs real-time
heroku logs --tail -a bfe-foraiot

# Tìm dòng Redis connection:
# ... INFO  RedisConfig - Connected to Redis at ...
```

### Test 3: Test WebSocket từ frontend
Mở trình duyệt:
```
https://bfe-foraiot.herokuapp.com/dashboard-realtime.html
```

Mở Console (F12) → Xem:
```
Connected to WebSocket
Subscribed to /topic/helmet/data
```

---

## 🐛 Troubleshooting

### Lỗi: Redis connection refused
```bash
# Kiểm tra Redis addon status
heroku addons:info heroku-redis -a bfe-foraiot

# Nếu State: creating → Đợi vài phút
# Nếu State: errored → Xóa và tạo lại:
heroku addons:destroy heroku-redis -a bfe-foraiot
heroku addons:create heroku-redis:mini -a bfe-foraiot
```

### Lỗi: Spring Boot không connect Redis
```bash
# Xem config vars
heroku config -a bfe-foraiot

# Kiểm tra có REDIS_URL không?
# Nếu không có:
heroku addons:create heroku-redis:mini -a bfe-foraiot
```

### Lỗi: WebSocket 503 Service Unavailable
```bash
# Xem logs
heroku logs --tail -a bfe-foraiot | findstr "WebSocket"

# Restart app
heroku restart -a bfe-foraiot
```

### Kiểm tra Redis credentials
```bash
heroku redis:credentials -a bfe-foraiot

# Output:
# Connection info string:
# redis://h:p1234567890abcdef@ec2-xx-xx-xx-xx.compute-1.amazonaws.com:12345
```

---

## 📊 Monitoring Redis

### Xem Redis stats
```bash
heroku redis:info -a bfe-foraiot

# Output:
# Plan:        mini
# Status:      available
# Connections: 2
# Memory:      10.52MB / 25MB
# ...
```

### Xem Redis metrics (qua Dashboard)
1. Heroku Dashboard → **bfe-foraiot**
2. Tab **Resources**
3. Click vào **Heroku Redis**
4. Xem graphs: Memory usage, Connection count, Hit rate

---

## 🔄 Migration Local → Heroku

**KHÔNG CẦN MIGRATION!**

Vì:
1. Redis chỉ dùng cho **Pub/Sub tạm thời** (in-memory messaging)
2. Dữ liệu thực được lưu trong **MySQL (JawsDB)**
3. Khi restart app → Redis data mất → OK vì chỉ là message queue

Nếu cần persistent data → Dùng **Heroku Redis Premium** (có RDB persistence)

---

## 📝 Tóm tắt Commands

```bash
# 1. Thêm Redis addon
heroku addons:create heroku-redis:mini -a bfe-foraiot

# 2. Kiểm tra Redis
heroku addons:info heroku-redis -a bfe-foraiot

# 3. Xem config
heroku config -a bfe-foraiot | findstr REDIS

# 4. Deploy
git push heroku main

# 5. Test Redis CLI
heroku redis:cli -a bfe-foraiot

# 6. Xem logs
heroku logs --tail -a bfe-foraiot

# 7. Restart app (nếu cần)
heroku restart -a bfe-foraiot

# 8. Xem Redis stats
heroku redis:info -a bfe-foraiot
```

---

## 🎯 Next Steps

Sau khi Redis hoạt động:
1. ✅ Test MQTT message từ Gateway → Backend nhận được
2. ✅ Backend lưu DB (JawsDB MySQL)
3. ✅ Backend publish Redis channel `helmet:data`
4. ✅ RedisSubscriber nhận message
5. ✅ Push qua WebSocket `/topic/helmet/data`
6. ✅ Frontend `dashboard-realtime.html` nhận real-time update

---

## 📚 Tài liệu tham khảo
- Heroku Redis Docs: https://devcenter.heroku.com/articles/heroku-redis
- Spring Data Redis: https://docs.spring.io/spring-data/redis/docs/current/reference/html/
- WebSocket (STOMP): https://spring.io/guides/gs/messaging-stomp-websocket/

---

## ✅ Checklist Deploy

- [ ] Redis addon đã tạo: `heroku addons:info heroku-redis -a bfe-foraiot`
- [ ] REDIS_URL đã có: `heroku config -a bfe-foraiot | findstr REDIS`
- [ ] Code đã push: `git push heroku main`
- [ ] App đã running: `heroku ps -a bfe-foraiot`
- [ ] Redis CLI connect OK: `heroku redis:cli -a bfe-foraiot` → `PING` → `PONG`
- [ ] Backend logs không lỗi: `heroku logs --tail -a bfe-foraiot`
- [ ] WebSocket dashboard hoạt động: `https://bfe-foraiot.herokuapp.com/dashboard-realtime.html`

---

**🎉 DONE! Redis trên Heroku đã sẵn sàng cho real-time WebSocket push!**
