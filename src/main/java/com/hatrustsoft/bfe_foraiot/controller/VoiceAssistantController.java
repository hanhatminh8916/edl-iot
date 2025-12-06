package com.hatrustsoft.bfe_foraiot.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * 🎤 VOICE ASSISTANT PROXY CONTROLLER
 * 
 * Proxy để gọi Gemini API từ backend thay vì client (tránh CORS)
 * Client sẽ gọi endpoint này, backend sẽ forward request đến Gemini API
 */
@RestController
@RequestMapping("/api/voice-assistant")
@Slf4j
public class VoiceAssistantController {
    
    private final WebClient webClient;
    
    public VoiceAssistantController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }
    
    /**
     * Proxy endpoint cho Gemini API generateContent
     * POST /api/voice-assistant/gemini
     */
    @PostMapping("/gemini")
    public Mono<String> proxyGeminiRequest(
            @RequestHeader("X-API-Key") String apiKey,
            @RequestBody String requestBody
    ) {
        log.info("🎤 Proxying request to Gemini API");
        
        // CRITICAL FIX: Dùng gemini-2.5-flash (model mới nhất, STABLE)
        // gemini-2.0-flash-exp: quota=0 cho free tier → LỖI 429 NGAY
        // gemini-1.5/2.0-flash: tự động redirect sang 2.0-flash-exp → LỖI 429
        // gemini-2.5-flash: Stable, free tier support, KHÔNG bị redirect
        String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;
        
        log.info("📤 Gemini Model: gemini-2.5-flash");
        log.info("📤 Request URL: {}", geminiUrl.replace(apiKey, "***KEY***"));
        
        return webClient.post()
                .uri(geminiUrl)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(error -> {
                    if (error instanceof WebClientResponseException webEx) {
                        String errorBody = webEx.getResponseBodyAsString();
                        if (webEx.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                            log.error("❌ 429 Rate Limit Error:");
                            log.error("   Status: {}", webEx.getStatusCode());
                            log.error("   Headers: {}", webEx.getHeaders());
                            log.error("   Body: {}", errorBody);
                            log.error("   💡 Giải pháp:");
                            log.error("      1. Tạo Google Cloud Project MỚI: https://console.cloud.google.com");
                            log.error("      2. Enable Generative Language API");
                            log.error("      3. Tạo API key MỚI từ project MỚI");
                            log.error("      4. KHÔNG enable experimental APIs");
                        } else {
                            log.error("❌ Gemini API error {}: {}", webEx.getStatusCode(), errorBody);
                        }
                    } else {
                        log.error("❌ Gemini API error: {}", error != null ? error.getMessage() : "Unknown error");
                    }
                })
                .doOnSuccess(response -> log.info("✅ Gemini API response received"));
    }
}
