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
    private static final long CACHE_TTL_SECONDS = 30; // 30 giây - nếu không nhận data thì xóa

    /**
     * Lưu helmet data vào Redis cache
     * Key: helmet:realtime:{MAC}
     * TTL: 30 giây (tự động xóa nếu không update)
     */
    public void cacheHelmetData(HelmetData data) {
        try {
            String key = HELMET_CACHE_PREFIX + data.getMac();
            redisTemplate.opsForValue().set(key, data, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            log.debug("💾 Cached helmet data: {} (TTL: {}s)", data.getMac(), CACHE_TTL_SECONDS);
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
}
