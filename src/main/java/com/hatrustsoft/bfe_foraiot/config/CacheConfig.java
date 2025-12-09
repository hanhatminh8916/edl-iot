package com.hatrustsoft.bfe_foraiot.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 🚀 Spring Cache Configuration for Positioning Optimization
 * 
 * Reduces positioning-2d.html from 60-80 queries per F5 to ~1 query
 * 
 * Cache Strategy:
 * - tagPositions: Cache all tag positions for 10 seconds
 * - offlineTags: Cache offline tag list for 10 seconds  
 * - allTags: Cache raw TagLastPosition entities for 10 seconds
 * 
 * Note: This is separate from Hibernate 2nd level cache (configured in ehcache.xml)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "tagPositions",
            "offlineTags", 
            "allTags"
        );
        
        // Configure TTL: 10 seconds for all caches
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .maximumSize(1000)
            .recordStats()); // Enable metrics
        
        return cacheManager;
    }
}
