package com.hatrustsoft.bfe_foraiot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.hatrustsoft.bfe_foraiot.entity.SafeZone;

import lombok.extern.slf4j.Slf4j;

/**
 * SafeZone Publisher
 * Push SafeZone updates qua WebSocket khi có thay đổi
 */
@Service
@Slf4j
public class SafeZonePublisher {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast SafeZone update qua WebSocket
     */
    public void publishSafeZoneUpdate(SafeZone safeZone, String action) {
        try {
            SafeZoneUpdateMessage message = new SafeZoneUpdateMessage();
            message.setAction(action); // CREATE, UPDATE, DELETE
            message.setSafeZone(safeZone);
            
            // Push qua WebSocket đến tất cả clients
            messagingTemplate.convertAndSend("/topic/safezone/update", message);
            
            log.info("📡 Published SafeZone {} to WebSocket: ID={}", action, safeZone.getId());
            
        } catch (Exception e) {
            log.error("❌ Error publishing SafeZone update: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Message wrapper cho SafeZone updates
     */
    public static class SafeZoneUpdateMessage {
        private String action; // CREATE, UPDATE, DELETE
        private SafeZone safeZone;
        
        public String getAction() {
            return action;
        }
        
        public void setAction(String action) {
            this.action = action;
        }
        
        public SafeZone getSafeZone() {
            return safeZone;
        }
        
        public void setSafeZone(SafeZone safeZone) {
            this.safeZone = safeZone;
        }
    }
}
