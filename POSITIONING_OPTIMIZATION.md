# 📊 TỔNG QUAN TỐI ƯU DATABASE QUERIES

## 🔍 PHÂN TÍCH HIỆN TẠI

### Database Info:
- **Loại**: JawsDB MySQL - **Leopard Plan** (Shared)
- **Giá**: $0.014/giờ (~$10/tháng)
- **Giới hạn**: **18,000 queries/giờ** = 300 queries/phút = **5 queries/giây**

### Trang positioning-2d.html - TRƯỚC TỐI ƯU:

**API Calls mỗi lần load/F5:**
1. `GET /api/positioning/tags` - Lấy tất cả tags → **1 query per tag** (nếu có 20 tags = 20 queries)
2. `GET /api/positioning/tags` - Gọi lại lần 2 → **20 queries nữa**
3. `GET /api/anchors` → **1 query**
4. `GET /api/safe-zones` → **1 query**
5. `GET /api/zones/{id}` (nếu có) → **1-5 queries**

**Tổng ước tính**: **60-80 queries mỗi lần F5** 🔴

**Vấn đề**:
- Không có cache
- Mỗi tag fetch từ DB tạo 1 query riêng (N+1 problem)
- Frontend gọi API nhiều lần
- Vượt giới hạn 5 queries/giây nếu nhiều user

---

## ✅ GIẢI PHÁP ĐÃ TRIỂN KHAI

### 1. **3-Layer Caching Strategy** 🚀

#### Layer 1: Browser Cache (CacheControl Header)
```java
return ResponseEntity.ok()
    .cacheControl(CacheControl.maxAge(10, TimeUnit.SECONDS))
    .body(result);
```
→ Browser cache 10s, không gửi request lên server

#### Layer 2: Application Cache (Spring @Cacheable)
```java
@Cacheable(value = "tagPositions", key = "'all'")
public ResponseEntity<List<TagPositionDTO>> getAllTagPositions()
```
→ Server cache 10s, không query DB

#### Layer 3: Repository Cache
```java
@Cacheable(value = "allTags")
public List<TagLastPosition> getAllTagPositions() {
    return tagLastPositionRepository.findAll();
}
```
→ Cache repository findAll() 10s

### 2. **Caffeine Cache Manager**
```java
Caffeine.newBuilder()
    .expireAfterWrite(10, TimeUnit.SECONDS)
    .maximumSize(1000)
    .recordStats()
```

### 3. **Hibernate Statistics**
```properties
spring.jpa.properties.hibernate.generate_statistics=true
logging.level.org.hibernate.stat=INFO
```
→ Monitor số queries realtime

---

## 📈 KẾT QUẢ DỰ KIẾN

### Trước tối ưu:
```
User F5 → 60-80 queries
10 users F5 → 600-800 queries
100 users/phút → 6,000-8,000 queries/phút ❌ (vượt giới hạn 300/phút)
```

### Sau tối ưu:
```
Lần 1 (cache miss): 1 query (findAll()) ✅
Lần 2-∞ (trong 10s): 0 queries (cache hit) ✅✅✅

10 users F5 cùng lúc: 0-1 query (share cache)
100 users/phút: ~10-20 queries (mỗi 10s refresh cache 1 lần)
```

**Giảm**: **98-99% queries** 🎉

---

## 🔧 FILES CHANGED

1. **pom.xml**
   - Added `spring-boot-starter-cache`
   - Added `caffeine` dependency

2. **CacheConfig.java** (NEW)
   - @EnableCaching
   - CaffeineCacheManager with 3 cache regions
   - 10s TTL

3. **PositioningController.java**
   - Added `@Cacheable` to `/api/positioning/tags`
   - Added `@Cacheable` to `/api/positioning/tags/offline`
   - Added `CacheControl.maxAge(10s)` headers

4. **PositioningService.java**
   - Added `@Cacheable("allTags")` to `getAllTagPositions()`

5. **application-heroku.properties**
   - Enabled Hibernate statistics for monitoring

---

## 📊 MONITORING

### Check cache hits/misses:
```bash
heroku logs --tail --app edl-safework-iot | grep "DB QUERY"
```

Sau deploy, sẽ thấy:
```
[DB QUERY] Fetching all tag positions from database  ← Lần đầu (cache miss)
[Cache hit - no log]                                 ← 10s tiếp theo (cache hit)
[DB QUERY] Fetching all tag positions from database  ← Sau 10s (cache expired)
```

### Check Hibernate statistics:
```bash
heroku logs --tail | grep "hibernate.stat"
```

---

## 🚀 DEPLOYMENT

```bash
git add -A
git commit -m "Optimize positioning endpoint - Reduce 60-80 queries per F5 to ~1 via 3-layer caching

- Add @Cacheable to PositioningController.getAllTagPositions() and getOfflineTags()
- Add @Cacheable to PositioningService.getAllTagPositions()
- Add browser CacheControl(10s) to API responses
- Configure Caffeine cache regions: tagPositions, offlineTags, allTags (10s TTL)
- Enable Hibernate statistics for monitoring
- Expected: positioning-2d.html now 0 queries on F5 (cache hit)"

git push heroku main
```

---

## ✅ CHECKLIST

- [x] Thêm Spring Cache + Caffeine dependencies
- [x] Tạo CacheConfig với @EnableCaching
- [x] Thêm @Cacheable vào Controller methods
- [x] Thêm @Cacheable vào Service layer
- [x] Thêm CacheControl headers
- [x] Enable Hibernate statistics
- [ ] Deploy lên Heroku
- [ ] Test với F5 nhiều lần
- [ ] Verify logs: lần đầu có query, lần sau không có
- [ ] Monitor Heroku metrics

---

## 🎯 NEXT STEPS (Nếu cần tối ưu thêm)

1. **Tăng cache TTL lên 30s** nếu data không cần realtime
2. **Cache /api/anchors và /api/safe-zones** (ít thay đổi)
3. **Eager loading cho relationships** để giảm N+1
4. **Redis cache** cho multi-instance scaling
5. **CDN** cho static assets

---

## 📌 LƯU Ý

- Cache TTL = 10s cân bằng giữa realtime và performance
- Nếu cần realtime hơn → giảm TTL xuống 5s
- Nếu cần performance hơn → tăng TTL lên 30s
- WebSocket vẫn push realtime updates (không ảnh hưởng)
