package com.hatrustsoft.bfe_foraiot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.hatrustsoft.bfe_foraiot.model.Alert;

import lombok.extern.slf4j.Slf4j;

/**
 * Alert Publisher
 * Push alerts qua WebSocket khi có cảnh báo mới hoặc cập nhật
 */
@Service
@Slf4j
public class AlertPublisher {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast alert mới qua WebSocket
     */
    public void publishNewAlert(Alert alert) {
        try {
            // Push qua WebSocket đến tất cả clients
            messagingTemplate.convertAndSend("/topic/alerts/new", alert);
            
            log.info("📡 Published new alert to WebSocket: ID={}, Type={}", 
                alert.getId(), alert.getAlertType());
            
        } catch (Exception e) {
            log.error("❌ Error publishing new alert: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Broadcast alert status update qua WebSocket
     */
    public void publishAlertUpdate(Alert alert) {
        try {
            // Push qua WebSocket đến tất cả clients
            messagingTemplate.convertAndSend("/topic/alerts/update", alert);
            
            log.info("📡 Published alert update to WebSocket: ID={}, Status={}", 
                alert.getId(), alert.getStatus());
            
        } catch (Exception e) {
            log.error("❌ Error publishing alert update: {}", e.getMessage(), e);
        }
    }
}
