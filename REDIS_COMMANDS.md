# Redis Management Scripts for BFE_forAIOT

## Development (Local Redis with Docker)

### Start Redis
```powershell
docker-compose up -d
```

### Stop Redis
```powershell
docker-compose down
```

### View Redis logs
```powershell
docker logs redis-bfe -f
```

### Test Redis connection
```powershell
docker exec redis-bfe redis-cli ping
# Output: PONG
```

### Connect to Redis CLI
```powershell
docker exec -it redis-bfe redis-cli
# Trong Redis CLI:
# > PING
# > KEYS *
# > SUBSCRIBE helmet:data
```

### Monitor Redis in real-time
```powershell
docker exec -it redis-bfe redis-cli MONITOR
```

---

## Production (Heroku Redis)

### 1. Add Redis addon to Heroku
```bash
heroku addons:create heroku-redis:mini -a your-app-name

# Hoặc qua Heroku Dashboard:
# Resources → Add-ons → Search "Heroku Redis" → Select "Mini" (Free)
```

### 2. Verify Redis addon
```bash
heroku addons:info heroku-redis -a your-app-name

# Output:
# === redis-whatever-12345
# Plan:        heroku-redis:mini
# Price:       free
# State:       created
```

### 3. Check Redis config vars (tự động set)
```bash
heroku config -a your-app-name | findstr REDIS

# Output:
# REDIS_TLS_URL: rediss://...
# REDIS_URL:     redis://...
```

### 4. Update application.properties for Heroku

**IMPORTANT:** Heroku tự động set biến môi trường, KHÔNG cần sửa code!

application.properties đã có sẵn:
```properties
# Local development
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=

# Heroku sẽ tự động override bằng REDIS_URL
```

### 5. Deploy to Heroku
```bash
git add .
git commit -m "Add Redis + WebSocket support"
git push heroku main

# Hoặc qua GitHub:
# Heroku Dashboard → Deploy → GitHub → Deploy Branch
```

### 6. Test Redis on Heroku
```bash
# Connect to Heroku Redis CLI
heroku redis:cli -a your-app-name

# Trong Redis CLI:
# > PING
# > SUBSCRIBE helmet:data
```

### 7. Monitor Redis on Heroku
```bash
# View Redis stats
heroku redis:info -a your-app-name

# View Redis credentials
heroku redis:credentials -a your-app-name
```

---

## Troubleshooting

### Local: Redis connection refused
```powershell
# Kiểm tra Redis container
docker ps --filter "name=redis-bfe"

# Nếu không chạy:
docker start redis-bfe

# Hoặc chạy lại:
docker-compose up -d
```

### Heroku: Redis connection timeout
```bash
# Kiểm tra addon status
heroku addons:info heroku-redis -a your-app-name

# Xem logs
heroku logs --tail -a your-app-name | findstr Redis
```

### Clear all Redis data (Development only!)
```powershell
docker exec redis-bfe redis-cli FLUSHALL
```

---

## Monitoring

### Local Redis stats
```powershell
docker exec redis-bfe redis-cli INFO stats
```

### Heroku Redis stats
```bash
heroku redis:info -a your-app-name
```

---

## Useful Commands

### List all keys in Redis
```powershell
docker exec redis-bfe redis-cli KEYS "*"
```

### Subscribe to helmet:data channel
```powershell
docker exec -it redis-bfe redis-cli
> SUBSCRIBE helmet:data
```

### Publish test message
```powershell
docker exec redis-bfe redis-cli PUBLISH helmet:data '{"mac":"TEST","battery":85}'
```

---

## Migration từ Local → Heroku

Không cần migration! Vì:
1. Redis chỉ dùng cho Pub/Sub (tạm thời)
2. Data thực được lưu trong MySQL (JawsDB)
3. Heroku Redis tự động config khi deploy

---

## Cleanup

### Remove local Redis (khi không dùng nữa)
```powershell
docker-compose down -v
docker rmi redis:alpine
```
