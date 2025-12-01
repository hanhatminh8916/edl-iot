package com.hatrustsoft.bfe_foraiot.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.hatrustsoft.bfe_foraiot.entity.HelmetData;

import lombok.extern.slf4j.Slf4j;

/**
 * Redis Cache Service
 * Store and retrieve helmet realtime data from Redis
 */
@Service
@Slf4j
public class RedisCacheService {

    @Autowired
    private RedisTemplate<String, HelmetData> redisTemplate;

    private static final String HELMET_CACHE_PREFIX = "helmet:realtime:";
    private static final long CACHE_TTL_SECONDS = 43200; // 12 giờ - sau đó xóa hoàn toàn

    /**
     * Lưu helmet data vào Redis cache
     * Key: helmet:realtime:{MAC}
     * TTL: 12 giờ (tự động xóa sau 12h không nhận data)
     */
    public void cacheHelmetData(HelmetData data) {
        try {
            // ⏰ Update receivedAt to current server time
            data.setReceivedAt(com.hatrustsoft.bfe_foraiot.util.VietnamTimeUtils.now());
            
            String key = HELMET_CACHE_PREFIX + data.getMac();
            redisTemplate.opsForValue().set(key, data, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            log.debug("💾 Cached helmet data: {} at {} (TTL: {}s)", data.getMac(), data.getReceivedAt(), CACHE_TTL_SECONDS);
        } catch (Exception e) {
            log.error("❌ Error caching helmet data: {}", e.getMessage(), e);
        }
    }

    /**
     * Lấy helmet data từ Redis cache
     */
    public HelmetData getHelmetData(String mac) {
        try {
            String key = HELMET_CACHE_PREFIX + mac;
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("❌ Error getting helmet data from cache: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Lấy TẤT CẢ helmet data đang active từ Redis
     */
    public List<HelmetData> getAllActiveHelmets() {
        try {
            Set<String> keys = redisTemplate.keys(HELMET_CACHE_PREFIX + "*");
            List<HelmetData> result = new ArrayList<>();
            
            if (keys != null) {
                for (String key : keys) {
                    HelmetData data = redisTemplate.opsForValue().get(key);
                    if (data != null) {
                        result.add(data);
                    }
                }
            }
            
            log.info("📊 Retrieved {} active helmets from Redis cache", result.size());
            return result;
        } catch (Exception e) {
            log.error("❌ Error getting all helmets from cache: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Xóa helmet data khỏi cache (khi helmet offline)
     */
    public void removeHelmetData(String mac) {
        try {
            String key = HELMET_CACHE_PREFIX + mac;
            redisTemplate.delete(key);
            log.info("🗑️ Removed helmet from cache: {}", mac);
        } catch (Exception e) {
            log.error("❌ Error removing helmet data from cache: {}", e.getMessage(), e);
        }
    }

    /**
     * 🔍 Lấy danh sách MAC của các helmet OFFLINE (không có data trong 30 giây)
     * @return List các HelmetData đã offline
     */
    public List<HelmetData> getOfflineHelmets(int timeoutSeconds) {
        List<HelmetData> offlineHelmets = new ArrayList<>();
        try {
            Set<String> keys = redisTemplate.keys(HELMET_CACHE_PREFIX + "*");
            if (keys == null) return offlineHelmets;
            
            java.time.LocalDateTime threshold = com.hatrustsoft.bfe_foraiot.util.VietnamTimeUtils.now().minusSeconds(timeoutSeconds);
            
            for (String key : keys) {
                HelmetData data = redisTemplate.opsForValue().get(key);
                if (data != null && data.getReceivedAt() != null) {
                    if (data.getReceivedAt().isBefore(threshold)) {
                        offlineHelmets.add(data);
                        log.debug("⏰ Helmet {} is OFFLINE (last seen: {})", data.getMac(), data.getReceivedAt());
                    }
                }
            }
        } catch (Exception e) {
            log.error("❌ Error checking offline helmets: {}", e.getMessage(), e);
        }
        return offlineHelmets;
    }

    /**
     * 🗑️ Xóa TOÀN BỘ cache helmet
     */
    public void clearAllCache() {
        try {
            Set<String> keys = redisTemplate.keys(HELMET_CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("🗑️ Cleared {} helmet caches", keys.size());
            } else {
                log.info("ℹ️ No helmet cache to clear");
            }
        } catch (Exception e) {
            log.error("❌ Error clearing all cache: {}", e.getMessage(), e);
        }
    }
}

