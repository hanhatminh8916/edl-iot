package com.hatrustsoft.bfe_foraiot.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MessengerSetupService {

    @Value("${facebook.messenger.page-access-token:DISABLED}")
    private String pageAccessToken;

    private final WebClient webClient;

    public MessengerSetupService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Setup "Get Started" button - Hiện khi user mở chat lần đầu
     */
    public void setupGetStartedButton() {
        String url = "https://graph.facebook.com/v18.0/me/messenger_profile?access_token=" + pageAccessToken;

        Map<String, Object> payload = new HashMap<>();
        Map<String, String> getStarted = new HashMap<>();
        getStarted.put("payload", "GET_STARTED");
        payload.put("get_started", getStarted);

        try {
            String response = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("✅ Get Started button setup successfully: {}", response);
        } catch (Exception e) {
            log.error("❌ Error setting up Get Started button: {}", e.getMessage());
        }
    }

    /**
     * Setup Greeting Text - Lời chào khi user mở chat
     */
    public void setupGreeting() {
        String url = "https://graph.facebook.com/v18.0/me/messenger_profile?access_token=" + pageAccessToken;

        Map<String, Object> payload = new HashMap<>();
        Map<String, Object>[] greeting = new Map[]{
                Map.of(
                        "locale", "default",
                        "text", "Xin chào! 👋 Chào mừng bạn đến với hệ thống quản lý mũ bảo hộ thông minh. Nhấn 'Bắt đầu' để bắt đầu!"
                )
        };
        payload.put("greeting", greeting);

        try {
            String response = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("✅ Greeting text setup successfully: {}", response);
        } catch (Exception e) {
            log.error("❌ Error setting up greeting: {}", e.getMessage());
        }
    }

    /**
     * Setup Persistent Menu - Menu luôn hiển thị bên trái chat
     */
    public void setupPersistentMenu() {
        String url = "https://graph.facebook.com/v18.0/me/messenger_profile?access_token=" + pageAccessToken;

        Map<String, Object> payload = new HashMap<>();
        Map<String, Object>[] persistentMenu = new Map[]{
                Map.of(
                        "locale", "default",
                        "composer_input_disabled", false,
                        "call_to_actions", new Map[]{
                                Map.of(
                                        "type", "postback",
                                        "title", "🏠 Trang chủ",
                                        "payload", "MENU_HOME"
                                ),
                                Map.of(
                                        "type", "postback",
                                        "title", "📋 Hướng dẫn",
                                        "payload", "MENU_HELP"
                                ),
                                Map.of(
                                        "type", "postback",
                                        "title", "✅ Đăng ký nhận thông báo",
                                        "payload", "MENU_SUBSCRIBE"
                                ),
                                Map.of(
                                        "type", "postback",
                                        "title", "📊 Kiểm tra trạng thái",
                                        "payload", "MENU_STATUS"
                                ),
                                Map.of(
                                        "type", "web_url",
                                        "title", "🌐 Mở Dashboard",
                                        "url", "https://edl-safework-iot-bf3ee691c9f6.herokuapp.com/"
                                )
                        }
                )
        };
        payload.put("persistent_menu", persistentMenu);

        try {
            String response = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("✅ Persistent menu setup successfully: {}", response);
        } catch (Exception e) {
            log.error("❌ Error setting up persistent menu: {}", e.getMessage());
        }
    }

    /**
     * Setup tất cả cùng lúc
     */
    public void setupAll() {
        log.info("🚀 Setting up Messenger Profile...");
        setupGetStartedButton();
        setupGreeting();
        setupPersistentMenu();
        log.info("✅ All Messenger Profile settings completed!");
    }

    /**
     * Xóa tất cả settings (để test lại)
     */
    public void deleteAllSettings() {
        String url = "https://graph.facebook.com/v18.0/me/messenger_profile?access_token=" + pageAccessToken;

        Map<String, String[]> payload = Map.of(
                "fields", new String[]{"get_started", "greeting", "persistent_menu"}
        );

        try {
            String response = webClient.method(org.springframework.http.HttpMethod.DELETE)
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("✅ All settings deleted: {}", response);
        } catch (Exception e) {
            log.error("❌ Error deleting settings: {}", e.getMessage());
        }
    }
}
