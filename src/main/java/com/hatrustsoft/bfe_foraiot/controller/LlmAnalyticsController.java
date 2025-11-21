package com.hatrustsoft.bfe_foraiot.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hatrustsoft.bfe_foraiot.service.LlmAnalyticsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Controller để expose LLM Analytics API endpoints
 * Cho phép frontend gọi các tính năng AI/ML phân tích dữ liệu
 */
@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class LlmAnalyticsController {

    private final LlmAnalyticsService llmAnalyticsService;

    /**
     * Natural Language Query
     * POST /api/analytics/query
     * 
     * Body: {
     *   "query": "Có bao nhiêu cảnh báo hôm nay?",
     *   "executeQueries": true,
     *   "includeData": true
     * }
     */
    @PostMapping("/query")
    public Mono<ResponseEntity<Map<String, Object>>> queryNaturalLanguage(
            @RequestBody Map<String, Object> request) {
                String query = (String) request.get("query");
                // Accept both camelCase and snake_case
                Object exec1 = request.getOrDefault("executeQueries", request.get("execute_queries"));
                Object inc1 = request.getOrDefault("includeData", request.get("include_data"));

        boolean executeQueries = parseBoolean(exec1, true);
        boolean includeData = parseBoolean(inc1, true);

        log.info("🔍 NL Query received: {}", query);

        return llmAnalyticsService.queryNaturalLanguage(query, executeQueries, includeData)
                .map(ResponseEntity::ok)
                                .onErrorResume(error -> {
                                        log.error("❌ Query failed", error);
                                        return Mono.just(ResponseEntity.internalServerError()
                                                        .body(Map.of("detail", error.getMessage())));
                                });
    }

    /**
     * Auto-generate Insights
     * POST /api/analytics/insights
     * 
     * Body: {
     *   "timeRange": "30d",
     *   "department": "Xây dựng"
     * }
     */
    @PostMapping("/insights")
    public Mono<ResponseEntity<Map<String, Object>>> generateInsights(
            @RequestBody Map<String, Object> request) {
                // Accept both snake_case and camelCase keys
                Object tr = request.getOrDefault("timeRange", request.getOrDefault("time_range", "30d"));
                String timeRange = tr != null ? tr.toString() : "30d";
                String department = (String) (request.containsKey("department") ? request.get("department") : request.get("department"));
                // insight types (optional) - controller currently ignores them and passes defaults to service

        log.info("📊 Generating insights: timeRange={}, department={}", timeRange, department);

        return llmAnalyticsService.generateInsights(timeRange, department)
                .map(ResponseEntity::ok)
                                .onErrorResume(error -> {
                                        log.error("❌ Insights generation failed", error);
                                        return Mono.just(ResponseEntity.internalServerError()
                                                        .body(Map.of("detail", error.getMessage())));
                                });
    }

    /**
     * Root Cause Analysis
     * GET /api/analytics/root-cause/{alertId}
     */
    @GetMapping("/root-cause/{alertId}")
    public Mono<ResponseEntity<Map<String, Object>>> analyzeRootCause(
            @PathVariable Long alertId,
            @RequestParam(defaultValue = "true") boolean includeContext) {
        
        log.info("🔍 Root cause analysis: alertId={}", alertId);

        return llmAnalyticsService.analyzeRootCause(alertId, includeContext)
                .map(ResponseEntity::ok)
                                .onErrorResume(error -> {
                                        log.error("❌ Root cause analysis failed", error);
                                        return Mono.just(ResponseEntity.internalServerError()
                                                        .body(Map.of("detail", error.getMessage())));
                                });
    }

    /**
     * Risk Prediction
     * GET /api/analytics/risk-prediction/{workerId}
     */
    @GetMapping("/risk-prediction/{workerId}")
    public Mono<ResponseEntity<Map<String, Object>>> predictRisk(
            @PathVariable Long workerId,
            @RequestParam(defaultValue = "7") int horizonDays) {
        
        log.info("⚠️ Risk prediction: workerId={}, horizonDays={}", workerId, horizonDays);

        return llmAnalyticsService.predictRisk(workerId, horizonDays)
                .map(ResponseEntity::ok)
                                .onErrorResume(error -> {
                                        log.error("❌ Risk prediction failed", error);
                                        return Mono.just(ResponseEntity.internalServerError()
                                                        .body(Map.of("detail", error.getMessage())));
                                });
    }

    /**
     * Generate Report
     * POST /api/analytics/report
     * 
     * Body: {
     *   "reportType": "weekly",
     *   "timeRange": "7d",
     *   "audience": "management"
     * }
     */
    @PostMapping("/report")
    public Mono<ResponseEntity<Map<String, Object>>> generateReport(
            @RequestBody Map<String, Object> request) {
                // Accept both snake_case and camelCase
                Object rpt = request.getOrDefault("reportType", request.get("report_type"));
                Object tr = request.getOrDefault("timeRange", request.get("time_range"));
                Object aud = request.getOrDefault("audience", request.get("audience"));

                String reportType = rpt != null ? rpt.toString() : "weekly";
                String timeRange = tr != null ? tr.toString() : "7d";
                String audience = aud != null ? aud.toString() : "management";

        log.info("📄 Generating report: type={}, timeRange={}, audience={}", 
                reportType, timeRange, audience);

        return llmAnalyticsService.generateReport(reportType, timeRange, audience)
                .map(ResponseEntity::ok)
                                .onErrorResume(error -> {
                                        log.error("❌ Report generation failed", error);
                                        return Mono.just(ResponseEntity.internalServerError()
                                                        .body(Map.of("detail", error.getMessage())));
                                });
    }

    /**
     * Health check cho LLM API
     * GET /api/analytics/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "LLM Analytics",
                "version", "1.0.0",
                "timestamp", java.time.Instant.now().toString()
        ));
    }

        private boolean parseBoolean(Object o, boolean def) {
                if (o == null) return def;
                if (o instanceof Boolean) return (Boolean) o;
                try {
                        return Boolean.parseBoolean(o.toString());
                } catch (Exception e) {
                        return def;
                }
        }
}
