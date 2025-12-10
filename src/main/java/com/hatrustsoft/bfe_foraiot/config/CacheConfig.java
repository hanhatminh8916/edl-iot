package com.hatrustsoft.bfe_foraiot.config;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.extern.slf4j.Slf4j;

/**
 * 🚀 Spring Cache Configuration
 * 
 * Giảm database queries cho positioning-2d.html từ ~60-80 queries/F5 xuống ~1 query
 * 
 * Cache Strategy:
 * - tagPositions: Cache API response /api/positioning/tags (10s TTL)
 * - offlineTags: Cache API response /api/positioning/tags/offline (10s TTL)
 * - allTags: Cache repository findAll() (10s TTL)
 * 
 * Kết hợp với CacheControl header (10s) → Browser cache + Server cache = 0 queries on F5
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // Define cache names
        cacheManager.setCacheNames(Arrays.asList(
            "tagPositions",    // API /api/positioning/tags
            "offlineTags",     // API /api/positioning/tags/offline
            "allTags"          // Repository findAll()
        ));
        
        // Configure Caffeine with 20 seconds TTL (optimized for query limit)
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(20, TimeUnit.SECONDS)
            .maximumSize(1000)
            .recordStats()); // Enable stats for monitoring
        
        log.info("🚀 Cache Manager initialized with Caffeine (20s TTL - optimized)");
        log.info("📦 Cache regions: tagPositions, offlineTags, allTags");
        
        return cacheManager;
    }
}
