package com.hatrustsoft.bfe_foraiot.service;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Service để tích hợp với Safety Analytics LLM API
 * Cung cấp khả năng phân tích dữ liệu bằng Natural Language
 */
@Service
@Slf4j
public class LlmAnalyticsService {

    private final WebClient webClient;

    @Value("${llm.api.base-url:https://api.safety-analytics.com}")
    private String baseUrl;

    @Value("${llm.api.key:}")
    private String apiKey;

    @Value("${llm.api.timeout:30}")
    private int timeoutSeconds;

    public LlmAnalyticsService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Gửi câu hỏi Natural Language tới LLM API
     * 
     * @param query Câu hỏi (tiếng Việt hoặc English)
     * @param executeQueries Có thực thi SQL queries không
     * @param includeData Có trả về data không
     * @return Response từ LLM API
     */
    public Mono<Map<String, Object>> queryNaturalLanguage(
            String query, 
            boolean executeQueries, 
            boolean includeData) {
        
        log.info("🤖 Sending NL query to LLM API: {}", query);

        Map<String, Object> requestBody = Map.of(
                "query", query,
                "execute_queries", executeQueries,
                "include_data", includeData,
                "context", Map.of(
                        "current_dashboard", "safety-monitoring",
                        "filters", Map.of()
                )
        );

        return webClient.post()
                .uri(baseUrl + "/api/llm/query")
                .headers(headers -> {
                    if (apiKey != null && !apiKey.isEmpty()) {
                        headers.setBearerAuth(apiKey);
                    }
                })
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(error -> {
                                    log.error("❌ LLM API error: {}", error);
                                    return Mono.error(new RuntimeException("LLM API error: " + error));
                                })
                )
                .bodyToMono((Class<Map<String, Object>>) (Class<?>) Map.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .doOnSuccess(response -> log.info("✅ LLM response received: intent={}", 
                        response.get("intent")))
                .doOnError(error -> log.error("❌ LLM API call failed", error));
    }

    /**
     * Tự động tạo insights từ dữ liệu
     * 
     * @param timeRange Khoảng thời gian (7d, 30d, 90d)
     * @param department Phòng ban (optional)
     * @return Insights và recommendations
     */
    public Mono<Map<String, Object>> generateInsights(String timeRange, String department) {
        log.info("📊 Generating insights for timeRange={}, department={}", timeRange, department);

        Map<String, Object> requestBody = Map.of(
                "time_range", timeRange,
                "department", department != null ? department : "",
                "insight_types", new String[]{"trends", "anomalies", "predictions"}
        );

        return webClient.post()
                .uri(baseUrl + "/api/llm/insights")
                .headers(headers -> {
                    if (apiKey != null && !apiKey.isEmpty()) {
                        headers.setBearerAuth(apiKey);
                    }
                })
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono((Class<Map<String, Object>>) (Class<?>) Map.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .doOnSuccess(response -> log.info("✅ Insights generated: {} insights", 
                        ((java.util.List<?>) response.get("insights")).size()))
                .doOnError(error -> log.error("❌ Failed to generate insights", error));
    }

    /**
     * Phân tích nguyên nhân gốc rễ của một alert
     * 
     * @param alertId ID của alert
     * @param includeContext Có bao gồm context xung quanh không
     * @return Root cause analysis
     */
    public Mono<Map<String, Object>> analyzeRootCause(Long alertId, boolean includeContext) {
        log.info("🔍 Analyzing root cause for alertId={}", alertId);

        return webClient.post()
                .uri(baseUrl + "/api/llm/root-cause-analysis?alert_id=" + alertId + "&include_context=" + includeContext)
                .headers(headers -> {
                    if (apiKey != null && !apiKey.isEmpty()) {
                        headers.setBearerAuth(apiKey);
                    }
                })
                .retrieve()
                .bodyToMono((Class<Map<String, Object>>) (Class<?>) Map.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .doOnSuccess(response -> log.info("✅ Root cause analysis completed for alert {}", alertId))
                .doOnError(error -> log.error("❌ Root cause analysis failed", error));
    }

    /**
     * Dự đoán rủi ro cho một công nhân
     * 
     * @param workerId ID công nhân
     * @param horizonDays Số ngày dự đoán (default: 7)
     * @return Risk prediction
     */
    public Mono<Map<String, Object>> predictRisk(Long workerId, int horizonDays) {
        log.info("⚠️ Predicting risk for workerId={}, horizon={}days", workerId, horizonDays);

        return webClient.post()
                .uri(baseUrl + "/api/llm/predict-risk?worker_id=" + workerId + "&horizon_days=" + horizonDays)
                .headers(headers -> {
                    if (apiKey != null && !apiKey.isEmpty()) {
                        headers.setBearerAuth(apiKey);
                    }
                })
                .retrieve()
                .bodyToMono((Class<Map<String, Object>>) (Class<?>) Map.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .doOnSuccess(response -> log.info("✅ Risk prediction completed"))
                .doOnError(error -> log.error("❌ Risk prediction failed", error));
    }

    /**
     * Tạo báo cáo tự động
     * 
     * @param reportType Loại báo cáo (weekly, monthly, quarterly)
     * @param timeRange Khoảng thời gian
     * @param audience Đối tượng đọc (management, technical, regulatory)
     * @return Generated report
     */
    public Mono<Map<String, Object>> generateReport(
            String reportType, 
            String timeRange, 
            String audience) {
        
        log.info("📄 Generating {} report for {}", reportType, audience);

        return webClient.post()
                .uri(baseUrl + "/api/llm/generate-report?report_type=" + reportType + "&time_range=" + timeRange + "&audience=" + audience)
                .headers(headers -> {
                    if (apiKey != null && !apiKey.isEmpty()) {
                        headers.setBearerAuth(apiKey);
                    }
                })
                .retrieve()
                .bodyToMono((Class<Map<String, Object>>) (Class<?>) Map.class)
                .timeout(Duration.ofSeconds(60)) // Reports need more time
                .doOnSuccess(response -> log.info("✅ Report generated successfully"))
                .doOnError(error -> log.error("❌ Report generation failed", error));
    }
}
