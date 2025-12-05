package com.hatrustsoft.bfe_foraiot.service;

import com.hatrustsoft.bfe_foraiot.dto.PushSubscriptionRequest;
import com.hatrustsoft.bfe_foraiot.model.Alert;
import com.hatrustsoft.bfe_foraiot.entity.PushSubscription;
import com.hatrustsoft.bfe_foraiot.repository.PushSubscriptionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Security;
import java.util.List;

/**
 * 📱 WEB PUSH NOTIFICATION SERVICE
 * 
 * Gửi Push Notifications đến các thiết bị đã đăng ký (iPhone, Android, Desktop)
 * Sử dụng Web Push Protocol với VAPID authentication
 * 
 * Chỉ gửi push cho FALL và HELP_REQUEST (cảnh báo khẩn cấp)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WebPushService {
    
    private final PushSubscriptionRepository subscriptionRepository;
    
    @Value("${webpush.vapid.public-key}")
    private String vapidPublicKey;
    
    @Value("${webpush.vapid.private-key}")
    private String vapidPrivateKey;
    
    @Value("${webpush.vapid.subject:mailto:admin@edl-safework.com}")
    private String vapidSubject;
    
    private PushService pushService;
    
    @PostConstruct
    public void init() {
        try {
            // Add BouncyCastle provider for encryption
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            
            // Initialize Push Service with VAPID keys
            pushService = new PushService();
            pushService.setPublicKey(vapidPublicKey);
            pushService.setPrivateKey(vapidPrivateKey);
            pushService.setSubject(vapidSubject);
            
            log.info("✅ WebPushService initialized with VAPID keys");
            log.info("📱 VAPID Public Key: {}...", vapidPublicKey.substring(0, 20));
            
        } catch (Exception e) {
            log.error("❌ Failed to initialize WebPushService: {}", e.getMessage());
        }
    }
    
    /**
     * Lấy VAPID Public Key để gửi cho client
     */
    public String getVapidPublicKey() {
        return vapidPublicKey;
    }
    
    /**
     * Đăng ký subscription mới
     */
    @Transactional
    public PushSubscription subscribe(PushSubscriptionRequest request) {
        log.info("📱 New push subscription request: {}", request.getEndpoint().substring(0, 50) + "...");
        
        // Check if already exists
        var existing = subscriptionRepository.findByEndpoint(request.getEndpoint());
        if (existing.isPresent()) {
            // Update existing subscription
            var subscription = existing.get();
            subscription.setP256dhKey(request.getKeys().getP256dh());
            subscription.setAuthKey(request.getKeys().getAuth());
            subscription.setUserAgent(request.getUserAgent());
            subscription.setIsActive(true);
            subscription.setDeviceType(detectDeviceType(request.getUserAgent()));
            
            log.info("✅ Updated existing push subscription");
            return subscriptionRepository.save(subscription);
        }
        
        // Create new subscription
        var subscription = PushSubscription.builder()
                .endpoint(request.getEndpoint())
                .p256dhKey(request.getKeys().getP256dh())
                .authKey(request.getKeys().getAuth())
                .userAgent(request.getUserAgent())
                .deviceType(detectDeviceType(request.getUserAgent()))
                .isActive(true)
                .build();
        
        log.info("✅ Created new push subscription for {}", subscription.getDeviceType());
        return subscriptionRepository.save(subscription);
    }
    
    /**
     * Hủy subscription
     */
    @Transactional
    public void unsubscribe(String endpoint) {
        log.info("📱 Unsubscribe request for endpoint");
        subscriptionRepository.deactivateByEndpoint(endpoint);
    }
    
    /**
     * Gửi push notification cho tất cả subscribers
     * Chỉ gửi cho FALL và HELP_REQUEST
     */
    @Async
    public void sendAlertPush(Alert alert) {
        // Chỉ gửi push cho FALL và HELP_REQUEST
        String alertType = alert.getAlertType() != null ? alert.getAlertType().name() : "UNKNOWN";
        if (!"FALL".equals(alertType) && !"HELP_REQUEST".equals(alertType)) {
            log.debug("⏭️ Skipping push for alert type: {}", alertType);
            return;
        }
        
        log.info("📲 Sending push notification for {} alert", alertType);
        
        List<PushSubscription> subscriptions = subscriptionRepository.findByIsActiveTrue();
        
        if (subscriptions.isEmpty()) {
            log.info("📱 No active push subscriptions");
            return;
        }
        
        log.info("📱 Sending push to {} devices", subscriptions.size());
        
        // Build notification payload
        String title = "FALL".equals(alertType) ? "🚨 PHÁT HIỆN NGÃ!" : "🆘 YÊU CẦU TRỢ GIÚP!";
        String body = alert.getMessage() != null ? alert.getMessage() : 
                      (alertType + " - " + (alert.getHelmet() != null ? alert.getHelmet().getHelmetId() : "N/A"));
        
        String payload = String.format("""
            {
                "title": "%s",
                "body": "%s",
                "icon": "/images/icon-192.png",
                "badge": "/images/icon-72.png",
                "tag": "alert-%d",
                "requireInteraction": true,
                "vibrate": [200, 100, 200, 100, 200],
                "data": {
                    "alertId": %d,
                    "alertType": "%s",
                    "url": "/location.html"
                }
            }
            """, 
            title.replace("\"", "\\\""),
            body.replace("\"", "\\\"").replace("\n", " "),
            alert.getId(),
            alert.getId(),
            alertType
        );
        
        for (PushSubscription sub : subscriptions) {
            sendPushToSubscription(sub, payload);
        }
    }
    
    /**
     * Gửi push đến một subscription cụ thể
     */
    private void sendPushToSubscription(PushSubscription sub, String payload) {
        try {
            Subscription subscription = new Subscription(
                sub.getEndpoint(),
                new Subscription.Keys(sub.getP256dhKey(), sub.getAuthKey())
            );
            
            Notification notification = new Notification(subscription, payload);
            
            var response = pushService.send(notification);
            
            int statusCode = response.getStatusLine().getStatusCode();
            
            if (statusCode == 201 || statusCode == 200) {
                log.info("✅ Push sent successfully to {} device", sub.getDeviceType());
            } else if (statusCode == 410 || statusCode == 404) {
                // Subscription expired or invalid - deactivate it
                log.warn("⚠️ Subscription expired, deactivating: {}", sub.getDeviceType());
                subscriptionRepository.deactivateByEndpoint(sub.getEndpoint());
            } else {
                log.warn("⚠️ Push response status: {} for {}", statusCode, sub.getDeviceType());
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to send push to {}: {}", sub.getDeviceType(), e.getMessage());
            
            // If subscription is invalid, deactivate it
            if (e.getMessage() != null && (e.getMessage().contains("410") || e.getMessage().contains("expired"))) {
                subscriptionRepository.deactivateByEndpoint(sub.getEndpoint());
            }
        }
    }
    
    /**
     * Detect device type from User-Agent
     */
    private String detectDeviceType(String userAgent) {
        if (userAgent == null) return "UNKNOWN";
        
        userAgent = userAgent.toLowerCase();
        
        if (userAgent.contains("iphone") || userAgent.contains("ipad")) {
            return "IPHONE";
        } else if (userAgent.contains("android")) {
            return "ANDROID";
        } else if (userAgent.contains("windows") || userAgent.contains("macintosh") || userAgent.contains("linux")) {
            return "DESKTOP";
        }
        
        return "UNKNOWN";
    }
    
    /**
     * Lấy số lượng subscriptions active
     */
    public long getActiveSubscriptionCount() {
        return subscriptionRepository.countByIsActiveTrue();
    }
}
