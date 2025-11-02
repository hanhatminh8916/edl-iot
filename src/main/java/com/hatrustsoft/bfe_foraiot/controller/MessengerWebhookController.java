package com.hatrustsoft.bfe_foraiot.controller;

import com.hatrustsoft.bfe_foraiot.dto.MessengerWebhookDTO;
import com.hatrustsoft.bfe_foraiot.entity.MessengerUser;
import com.hatrustsoft.bfe_foraiot.service.MessengerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xử lý Webhook từ Facebook Messenger
 */
@RestController
@RequestMapping("/api/webhook")
@Slf4j
public class MessengerWebhookController {

    @Value("${facebook.messenger.verify-token}")
    private String verifyToken;

    private final MessengerService messengerService;

    public MessengerWebhookController(MessengerService messengerService) {
        this.messengerService = messengerService;
    }

    /**
     * Webhook verification - Facebook sẽ gọi endpoint này để verify
     * GET /api/webhook?hub.mode=subscribe&hub.challenge=123456&hub.verify_token=YOUR_VERIFY_TOKEN
     */
    @GetMapping
    public ResponseEntity<?> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.challenge") String challenge,
            @RequestParam("hub.verify_token") String token) {

        log.info("Webhook verification request - Mode: {}, Token: {}", mode, token);

        // Kiểm tra verify token
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            log.info("✅ Webhook verified successfully!");
            return ResponseEntity.ok(challenge);
        }

        log.error("❌ Webhook verification failed! Invalid token");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
    }

    /**
     * Nhận webhook events từ Facebook Messenger
     * POST /api/webhook
     */
    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody MessengerWebhookDTO webhook) {
        log.info("📩 Received webhook: {}", webhook);

        if (!"page".equals(webhook.getObject())) {
            log.warn("⚠️ Unknown webhook object type: {}", webhook.getObject());
            return ResponseEntity.ok("EVENT_RECEIVED");
        }

        // Xử lý từng entry
        webhook.getEntry().forEach(entry -> {
            // Xử lý từng messaging event
            entry.getMessaging().forEach(messaging -> {
                String senderId = messaging.getSender().getId();
                
                log.info("Processing message from sender: {}", senderId);

                // Lưu hoặc cập nhật user
                MessengerUser user = messengerService.saveOrUpdateUser(senderId);

                // Xử lý tin nhắn
                if (messaging.getMessage() != null && messaging.getMessage().getText() != null) {
                    handleMessage(senderId, messaging.getMessage().getText());
                }

                // Xử lý postback (khi user click button)
                if (messaging.getPostback() != null) {
                    handlePostback(senderId, messaging.getPostback().getPayload());
                }
            });
        });

        return ResponseEntity.ok("EVENT_RECEIVED");
    }

    /**
     * Xử lý tin nhắn text từ user
     */
    private void handleMessage(String senderId, String messageText) {
        log.info("Message from {}: {}", senderId, messageText);

        String responseText;

        // Xử lý các command
        switch (messageText.toLowerCase().trim()) {
            case "hi", "hello", "chào", "xin chào":
                responseText = "👋 Xin chào! Tôi là Bot quản lý mũ bảo hộ thông minh.\n\n" +
                        "Gõ 'help' để xem các lệnh có sẵn.";
                break;

            case "help", "trợ giúp":
                responseText = "📋 Các lệnh có sẵn:\n\n" +
                        "• 'subscribe' - Đăng ký nhận thông báo\n" +
                        "• 'unsubscribe' - Hủy nhận thông báo\n" +
                        "• 'status' - Kiểm tra trạng thái\n" +
                        "• 'link [mã nhân viên]' - Liên kết với mã nhân viên";
                break;

            case "subscribe", "đăng ký":
                responseText = "✅ Bạn đã đăng ký nhận thông báo cảnh báo nguy hiểm!";
                // TODO: Update user subscription status
                break;

            case "unsubscribe", "hủy":
                responseText = "❌ Bạn đã hủy đăng ký nhận thông báo.";
                // TODO: Update user subscription status
                break;

            case "status", "trạng thái":
                responseText = "📊 Trạng thái của bạn:\n\n" +
                        "✅ Đã đăng ký nhận thông báo\n" +
                        "🆔 Messenger ID: " + senderId;
                break;

            default:
                // Kiểm tra nếu là lệnh link
                if (messageText.toLowerCase().startsWith("link ")) {
                    String employeeId = messageText.substring(5).trim();
                    messengerService.linkUserToEmployee(senderId, employeeId);
                    responseText = "✅ Đã liên kết với mã nhân viên: " + employeeId;
                } else {
                    responseText = "🤔 Tôi không hiểu lệnh này. Gõ 'help' để xem hướng dẫn.";
                }
        }

        // Gửi response
        messengerService.sendTextMessage(senderId, responseText);
    }

    /**
     * Xử lý postback từ button clicks
     */
    private void handlePostback(String senderId, String payload) {
        log.info("Postback from {}: {}", senderId, payload);

        String responseText;

        switch (payload) {
            case "ALERT_HANDLED":
                responseText = "✅ Cảm ơn bạn đã xác nhận đã xử lý cảnh báo!";
                break;

            case "CALL_EMERGENCY":
                responseText = "📞 Đang gọi số khẩn cấp: 115\n\n" +
                        "Vui lòng báo cáo tình hình nguy hiểm!";
                break;

            case "VIEW_LOCATION":
                responseText = "📍 Xem vị trí chi tiết tại:\n" +
                        "https://your-dashboard-url.com/location";
                // TODO: Gửi link với location cụ thể
                break;

            default:
                responseText = "Đã nhận postback: " + payload;
        }

        messengerService.sendTextMessage(senderId, responseText);
    }

    /**
     * Test endpoint để gửi tin nhắn thủ công
     * POST /api/webhook/test-alert
     */
    @PostMapping("/test-alert")
    public ResponseEntity<String> testAlert(
            @RequestParam String recipientId,
            @RequestParam(defaultValue = "Nguyễn Văn A") String employeeName,
            @RequestParam(defaultValue = "Khí độc vượt ngưỡng") String alertType,
            @RequestParam(defaultValue = "Khu vực công trường A") String location) {

        log.info("Sending test alert to: {}", recipientId);

        messengerService.sendDangerAlert(recipientId, employeeName, alertType, location);

        return ResponseEntity.ok("Test alert sent successfully!");
    }

    /**
     * Broadcast alert tới tất cả users
     * POST /api/webhook/broadcast-alert
     */
    @PostMapping("/broadcast-alert")
    public ResponseEntity<String> broadcastAlert(
            @RequestParam String employeeName,
            @RequestParam String alertType,
            @RequestParam String location) {

        log.info("Broadcasting alert - Employee: {}, Type: {}", employeeName, alertType);

        messengerService.broadcastDangerAlert(employeeName, alertType, location);

        return ResponseEntity.ok("Alert broadcasted successfully!");
    }
}
