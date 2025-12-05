package com.hatrustsoft.bfe_foraiot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity để lưu trữ Push Subscription từ các thiết bị đăng ký nhận thông báo
 * Hỗ trợ Web Push API cho PWA (iPhone, Android, Desktop)
 */
@Entity
@Table(name = "push_subscriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushSubscription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Endpoint URL từ Push Service (FCM, APNs, etc.)
     */
    @Column(nullable = false, length = 500)
    private String endpoint;
    
    /**
     * P256DH key cho encryption
     */
    @Column(name = "p256dh_key", nullable = false, length = 255)
    private String p256dhKey;
    
    /**
     * Auth secret cho encryption
     */
    @Column(name = "auth_key", nullable = false, length = 255)
    private String authKey;
    
    /**
     * User Agent để identify thiết bị
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    /**
     * Device type: IPHONE, ANDROID, DESKTOP
     */
    @Column(name = "device_type", length = 50)
    private String deviceType;
    
    /**
     * Thời gian đăng ký
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    /**
     * Thời gian cập nhật cuối
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Trạng thái active
     */
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
