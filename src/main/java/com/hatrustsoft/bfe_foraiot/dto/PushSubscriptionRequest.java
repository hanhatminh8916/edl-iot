package com.hatrustsoft.bfe_foraiot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho Push Subscription request từ client
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushSubscriptionRequest {
    
    /**
     * Endpoint URL từ Push Service
     */
    private String endpoint;
    
    /**
     * Keys object chứa p256dh và auth
     */
    private Keys keys;
    
    /**
     * User Agent string
     */
    private String userAgent;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Keys {
        private String p256dh;
        private String auth;
    }
}
