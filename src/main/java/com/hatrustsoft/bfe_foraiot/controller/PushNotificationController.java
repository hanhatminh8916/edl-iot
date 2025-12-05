package com.hatrustsoft.bfe_foraiot.controller;

import com.hatrustsoft.bfe_foraiot.dto.PushSubscriptionRequest;
import com.hatrustsoft.bfe_foraiot.entity.PushSubscription;
import com.hatrustsoft.bfe_foraiot.service.WebPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 📱 PUSH NOTIFICATION CONTROLLER
 * 
 * Endpoints:
 * - GET /api/push/vapid-key - Lấy VAPID public key
 * - POST /api/push/subscribe - Đăng ký nhận push notification
 * - POST /api/push/unsubscribe - Hủy đăng ký
 * - GET /api/push/status - Kiểm tra trạng thái
 */
@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PushNotificationController {
    
    private final WebPushService webPushService;
    
    /**
     * Lấy VAPID Public Key để client subscribe
     */
    @GetMapping("/vapid-key")
    public ResponseEntity<Map<String, String>> getVapidKey() {
        String publicKey = webPushService.getVapidPublicKey();
        return ResponseEntity.ok(Map.of("publicKey", publicKey));
    }
    
    /**
     * Đăng ký nhận push notification
     */
    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, Object>> subscribe(@RequestBody PushSubscriptionRequest request) {
        try {
            log.info("📱 Received push subscription request");
            
            if (request.getEndpoint() == null || request.getKeys() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid subscription data"
                ));
            }
            
            PushSubscription subscription = webPushService.subscribe(request);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đăng ký thông báo thành công!",
                "deviceType", subscription.getDeviceType(),
                "id", subscription.getId()
            ));
            
        } catch (Exception e) {
            log.error("❌ Failed to save subscription: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Lỗi đăng ký: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Hủy đăng ký push notification
     */
    @PostMapping("/unsubscribe")
    public ResponseEntity<Map<String, Object>> unsubscribe(@RequestBody Map<String, String> request) {
        try {
            String endpoint = request.get("endpoint");
            
            if (endpoint == null || endpoint.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Endpoint is required"
                ));
            }
            
            webPushService.unsubscribe(endpoint);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã hủy đăng ký thông báo"
            ));
            
        } catch (Exception e) {
            log.error("❌ Failed to unsubscribe: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Lỗi hủy đăng ký: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Kiểm tra trạng thái push notification
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        long activeCount = webPushService.getActiveSubscriptionCount();
        
        return ResponseEntity.ok(Map.of(
            "enabled", true,
            "activeSubscriptions", activeCount,
            "vapidConfigured", webPushService.getVapidPublicKey() != null
        ));
    }
}
