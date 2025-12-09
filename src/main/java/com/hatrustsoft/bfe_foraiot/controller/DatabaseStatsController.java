package com.hatrustsoft.bfe_foraiot.controller;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 📊 Database Statistics Controller
 * Monitor Hibernate queries và performance
 */
@RestController
@RequestMapping("/api/db-stats")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class DatabaseStatsController {

    private final EntityManagerFactory entityManagerFactory;

    /**
     * 📊 Lấy Hibernate statistics
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getStatistics() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();

        Map<String, Object> statsMap = new HashMap<>();
        
        // Query Statistics
        statsMap.put("queryExecutionCount", stats.getQueryExecutionCount());
        statsMap.put("queryExecutionMaxTime", stats.getQueryExecutionMaxTime());
        statsMap.put("queryExecutionMaxTimeQueryString", stats.getQueryExecutionMaxTimeQueryString());
        
        // Entity Statistics
        statsMap.put("entityLoadCount", stats.getEntityLoadCount());
        statsMap.put("entityFetchCount", stats.getEntityFetchCount());
        statsMap.put("entityInsertCount", stats.getEntityInsertCount());
        statsMap.put("entityUpdateCount", stats.getEntityUpdateCount());
        statsMap.put("entityDeleteCount", stats.getEntityDeleteCount());
        
        // Collection Statistics
        statsMap.put("collectionLoadCount", stats.getCollectionLoadCount());
        statsMap.put("collectionFetchCount", stats.getCollectionFetchCount());
        
        // Cache Statistics
        statsMap.put("secondLevelCacheHitCount", stats.getSecondLevelCacheHitCount());
        statsMap.put("secondLevelCacheMissCount", stats.getSecondLevelCacheMissCount());
        statsMap.put("secondLevelCachePutCount", stats.getSecondLevelCachePutCount());
        
        // Session Statistics
        statsMap.put("sessionOpenCount", stats.getSessionOpenCount());
        statsMap.put("sessionCloseCount", stats.getSessionCloseCount());
        statsMap.put("flushCount", stats.getFlushCount());
        
        // Transaction Statistics
        statsMap.put("transactionCount", stats.getTransactionCount());
        statsMap.put("successfulTransactionCount", stats.getSuccessfulTransactionCount());
        
        // Connection Statistics
        statsMap.put("connectCount", stats.getConnectCount());
        statsMap.put("prepareStatementCount", stats.getPrepareStatementCount());
        statsMap.put("closeStatementCount", stats.getCloseStatementCount());
        
        // Computed metrics
        long totalQueries = stats.getQueryExecutionCount() 
                          + stats.getEntityLoadCount() 
                          + stats.getCollectionLoadCount();
        statsMap.put("totalDatabaseAccess", totalQueries);
        
        // JawsDB Leopard limit check
        statsMap.put("jawsdbLeopardLimit", 18000);
        statsMap.put("percentageUsed", String.format("%.2f%%", (totalQueries / 18000.0) * 100));
        
        log.info("📊 DB Stats - Total queries: {}, Entity loads: {}, Collections: {}", 
            stats.getQueryExecutionCount(), 
            stats.getEntityLoadCount(), 
            stats.getCollectionLoadCount());

        return ResponseEntity.ok(statsMap);
    }

    /**
     * 🔄 Reset statistics counter
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetStatistics() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        
        stats.clear();
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Statistics reset successfully");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        
        log.info("🔄 Hibernate statistics reset");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 📈 Lấy query execution breakdown
     */
    @GetMapping("/queries")
    public ResponseEntity<Map<String, Object>> getQueryBreakdown() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();

        Map<String, Object> breakdown = new HashMap<>();
        
        // All executed queries
        String[] queries = stats.getQueries();
        Map<String, Object> queriesDetail = new HashMap<>();
        
        for (String query : queries) {
            org.hibernate.stat.QueryStatistics qStats = stats.getQueryStatistics(query);
            Map<String, Object> qDetail = new HashMap<>();
            qDetail.put("executionCount", qStats.getExecutionCount());
            qDetail.put("executionMaxTime", qStats.getExecutionMaxTime());
            qDetail.put("executionMinTime", qStats.getExecutionMinTime());
            qDetail.put("executionAvgTime", qStats.getExecutionAvgTime());
            qDetail.put("cacheHitCount", qStats.getCacheHitCount());
            qDetail.put("cacheMissCount", qStats.getCacheMissCount());
            
            queriesDetail.put(query.substring(0, Math.min(100, query.length())) + "...", qDetail);
        }
        
        breakdown.put("totalQueryTypes", queries.length);
        breakdown.put("queries", queriesDetail);
        
        return ResponseEntity.ok(breakdown);
    }
}
