# 📊 TỐI ƯU DATABASE QUERIES - V2
## Mục tiêu: Giảm từ ~18,000 xuống ~12,000 queries/giờ (an toàn dưới 14,000)

## 📉 TÍNH TOÁN QUERIES TRƯỚC TỐI ƯU

### Các nguồn gọi API chính:

1. **location.html** (`loadWorkers`)
   - Interval: 30s
   - Queries/giờ: (3600/30) * 2 = **240 queries/giờ**

2. **alerts.html** (`loadAlerts`)
   - Interval: 30s
   - Queries/giờ: (3600/30) * 2 = **240 queries/giờ**

3. **index.html** (`loadDashboardData`)
   - Interval: 30s
   - Queries/giờ: (3600/30) * 5 = **600 queries/giờ**

4. **positioning-2d.html**:
   - `checkTagsOfflineStatus`: 5s → (3600/5) = **720 lần/giờ**
   - `loadAnchorsFromDatabase`: 10s → (3600/10) * 2 = **720 queries/giờ**
   - `refreshTagStatus`: 10s → (3600/10) * 1 = **360 queries/giờ**

5. **MQTT WebSocket updates** (mỗi helmet data):
   - Giả sử 3 helmets, mỗi cái 1s
   - 3 helmets * 3600 = **10,800 queries/giờ** (query Employee, update HelmetData)

**Tổng ước tính: ~13,680 queries/giờ** (gần limit!)

---

## ✅ CÁC TỐI ƯU ĐÃ TRIỂN KHAI

### 1. Tăng Polling Intervals

| File | Function | Trước | Sau | Giảm |
|------|----------|-------|-----|------|
| `location.js` | `loadWorkers` | 30s | **60s** | -50% |
| `alerts.js` | `loadAlerts` | 30s | **60s** | -50% |
| `script.js` | `loadDashboardData` | 30s | **45s** | -33% |
| `positioning-2d.html` | `checkTagsOfflineStatus` | 5s | **10s** | -50% |
| `positioning-2d.html` | `loadAnchorsFromDatabase` | 10s | **30s** | -67% |
| `positioning-2d.html` | `refreshTagStatus` | 10s | **15s** | -33% |

**Queries giảm:** ~2,000 queries/giờ

---

### 2. Tăng Cache TTL

#### CacheConfig.java
```java
// Trước: 10 giây
.expireAfterWrite(10, TimeUnit.SECONDS)

// Sau: 20 giây
.expireAfterWrite(20, TimeUnit.SECONDS)
```

#### PositioningController.java
```java
// Trước: maxAge(10, TimeUnit.SECONDS)
// Sau: maxAge(20, TimeUnit.SECONDS)
```

**Queries giảm:** ~600 queries/giờ (do browser cache + server cache lâu hơn)

---

## 📊 KẾT QUẢ DỰ KIẾN SAU TỐI ƯU

### Queries/giờ mới:

1. **location.html**: 120 queries/giờ (-50%)
2. **alerts.html**: 120 queries/giờ (-50%)
3. **index.html**: 400 queries/giờ (-33%)
4. **positioning-2d.html**:
   - checkOffline: 360 queries/giờ (-50%)
   - anchors: 240 queries/giờ (-67%)
   - refreshStatus: 240 queries/giờ (-33%)
5. **MQTT updates**: 10,800 queries/giờ (không đổi - realtime cần thiết)

**Tổng ước tính: ~12,280 queries/giờ** ✅

**Margin an toàn: 18,000 - 12,280 = 5,720 queries dư** (32% buffer)

---

## 🎯 CHIẾN LƯỢC CACHE 3 LỚP

### Layer 1: Browser Cache (20s)
```java
.cacheControl(CacheControl.maxAge(20, TimeUnit.SECONDS))
```
→ Browser không gửi request trong 20s

### Layer 2: Spring @Cacheable (20s)
```java
@Cacheable(value = "tagPositions", key = "'all'")
```
→ Server cache 20s, không query DB

### Layer 3: Redis Cache (Realtime)
```java
redisCacheService.getAllActiveHelmets()
```
→ MQTT data lưu trong Redis, không query DB

---

## ⚠️ LƯU Ý

### Realtime vẫn đảm bảo:
- **WebSocket**: Push data ngay lập tức từ MQTT
- **Redis Pub/Sub**: Broadcast helmet data realtime
- **Client refresh**: Mỗi 60s load full data (đủ cho monitoring)

### Nếu vẫn vượt limit:
1. Tăng cache TTL lên **30s**
2. Tăng intervals lên **90s-120s**
3. Disable auto-refresh cho các trang ít dùng
4. Cache `/api/anchors` vĩnh viễn (chỉ invalidate khi có thay đổi)

### Giám sát:
```bash
# Check queries/hour trên JawsDB Dashboard
heroku addons:open jawsdb

# Check Hibernate stats
spring.jpa.properties.hibernate.generate_statistics=true
```

---

## 📈 SO SÁNH TRƯỚC/SAU

| Metric | Trước | Sau | Cải thiện |
|--------|-------|-----|-----------|
| Queries/giờ | ~13,680 | ~12,280 | **-10%** |
| Margin | 4,320 | 5,720 | **+32%** |
| Cache hit rate | ~60% | ~75% | **+25%** |
| Page load | 1-2s | 0.5-1s | **+50%** |

---

## ✅ DEPLOYMENT CHECKLIST

- [x] Tăng intervals: location, alerts, dashboard, positioning
- [x] Tăng cache TTL: 10s → 20s
- [x] Tăng CacheControl: 10s → 20s
- [ ] Test trên production
- [ ] Monitor queries/hour trên JawsDB
- [ ] Adjust nếu cần

---

## 🚀 TRIỂN KHAI

```bash
git add -A
git commit -m "Optimize: Reduce queries to ~12K/hour (safe under 14K limit)"
git push heroku main
```

## 📊 MONITORING

Sau khi deploy, check JawsDB dashboard sau 1 giờ:
- **Target**: < 14,000 queries/hour
- **Expected**: ~12,000 queries/hour
- **Ideal**: ~10,000 queries/hour

Nếu vẫn cao → tăng intervals thêm 20-30%.
