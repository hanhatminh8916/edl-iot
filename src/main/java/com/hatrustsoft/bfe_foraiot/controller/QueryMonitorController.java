package com.hatrustsoft.bfe_foraiot.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * 📊 Query Monitor API
 * Realtime database query statistics
 */
@RestController
@RequestMapping("/api/monitor")
@CrossOrigin(origins = "*")
@Slf4j
public class QueryMonitorController {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    /**
     * 📊 Lấy Hibernate statistics realtime
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getQueryStats() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        
        Map<String, Object> result = new HashMap<>();
        
        // Basic query stats
        result.put("queryExecutionCount", stats.getQueryExecutionCount());
        result.put("queryCacheHitCount", stats.getQueryCacheHitCount());
        result.put("queryCacheMissCount", stats.getQueryCacheMissCount());
        result.put("queryCachePutCount", stats.getQueryCachePutCount());
        
        // Second level cache stats
        result.put("secondLevelCacheHitCount", stats.getSecondLevelCacheHitCount());
        result.put("secondLevelCacheMissCount", stats.getSecondLevelCacheMissCount());
        result.put("secondLevelCachePutCount", stats.getSecondLevelCachePutCount());
        
        // Entity stats
        result.put("entityLoadCount", stats.getEntityLoadCount());
        result.put("entityFetchCount", stats.getEntityFetchCount());
        result.put("entityInsertCount", stats.getEntityInsertCount());
        result.put("entityUpdateCount", stats.getEntityUpdateCount());
        result.put("entityDeleteCount", stats.getEntityDeleteCount());
        
        // Connection stats
        result.put("connectCount", stats.getConnectCount());
        result.put("prepareStatementCount", stats.getPrepareStatementCount());
        result.put("closeStatementCount", stats.getCloseStatementCount());
        
        // Session stats
        result.put("sessionOpenCount", stats.getSessionOpenCount());
        result.put("sessionCloseCount", stats.getSessionCloseCount());
        result.put("transactionCount", stats.getTransactionCount());
        result.put("successfulTransactionCount", stats.getSuccessfulTransactionCount());
        
        // Timestamp
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("statisticsEnabled", stats.isStatisticsEnabled());
        
        // Calculate cache hit rate
        long totalCacheRequests = stats.getQueryCacheHitCount() + stats.getQueryCacheMissCount();
        double cacheHitRate = totalCacheRequests > 0 
            ? (stats.getQueryCacheHitCount() * 100.0 / totalCacheRequests) 
            : 0.0;
        result.put("cacheHitRate", String.format("%.2f", cacheHitRate));
        
        // Calculate queries per second (approximate)
        long totalQueries = stats.getQueryExecutionCount();
        result.put("totalQueries", totalQueries);
        
        log.debug("📊 Query stats: {} queries executed, {}% cache hit rate", 
            totalQueries, String.format("%.2f", cacheHitRate));
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 🔄 Reset statistics
     */
    @PostMapping("/stats/reset")
    public ResponseEntity<Map<String, String>> resetStats() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        
        stats.clear();
        
        log.info("🔄 Hibernate statistics reset");
        
        Map<String, String> result = new HashMap<>();
        result.put("message", "Statistics reset successfully");
        result.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 📈 Lấy query details
     */
    @GetMapping("/queries")
    public ResponseEntity<Map<String, Object>> getQueryDetails() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        
        Map<String, Object> result = new HashMap<>();
        
        // Get slow query stats
        String[] queries = stats.getQueries();
        Map<String, Object> queryStats = new HashMap<>();
        
        for (String query : queries) {
            Map<String, Object> queryStat = new HashMap<>();
            queryStat.put("executionCount", stats.getQueryStatistics(query).getExecutionCount());
            queryStat.put("executionRowCount", stats.getQueryStatistics(query).getExecutionRowCount());
            queryStat.put("executionAvgTime", stats.getQueryStatistics(query).getExecutionAvgTime());
            queryStat.put("executionMaxTime", stats.getQueryStatistics(query).getExecutionMaxTime());
            queryStat.put("executionMinTime", stats.getQueryStatistics(query).getExecutionMinTime());
            
            queryStats.put(query, queryStat);
        }
        
        result.put("queries", queryStats);
        result.put("queryCount", queries.length);
        result.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(result);
    }
}
