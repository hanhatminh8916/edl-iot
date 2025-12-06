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
        
        // Force sử dụng gemini-1.5-flash (free tier stable model)
        // Tránh gemini-2.0-flash-exp có quota = 0 cho free tier
        String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;
        
        log.debug("📤 Request to: {}", geminiUrl);
        
        return webClient.post()
                .uri(geminiUrl)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(error -> {
                    if (error instanceof WebClientResponseException webEx) {
                        if (webEx.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                            log.error("❌ 429 Rate Limit: {}. Hướng dẫn: Tạo project mới tại https://console.cloud.google.com hoặc nâng cấp lên paid tier", 
                                    webEx.getResponseBodyAsString());
                        } else {
                            log.error("❌ Gemini API error {}: {}", webEx.getStatusCode(), webEx.getResponseBodyAsString());
                        }
                    } else {
                        log.error("❌ Gemini API error: {}", error.getMessage());
                    }
                })
                .doOnSuccess(response -> log.info("✅ Gemini API response received"));
    }
}
