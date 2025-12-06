package com.hatrustsoft.bfe_foraiot.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
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
        
        String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;
        
        return webClient.post()
                .uri(geminiUrl)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(error -> log.error("❌ Gemini API error: {}", error.getMessage()))
                .doOnSuccess(response -> log.info("✅ Gemini API response received"));
    }
}
