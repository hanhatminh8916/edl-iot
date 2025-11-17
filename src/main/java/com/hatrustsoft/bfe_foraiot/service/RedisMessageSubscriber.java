package com.hatrustsoft.bfe_foraiot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hatrustsoft.bfe_foraiot.entity.HelmetData;

import lombok.extern.slf4j.Slf4j;

/**
 * Redis Message Subscriber
 * Subscribe messages from Redis và forward qua WebSocket
 */
@Service
@Slf4j
public class RedisMessageSubscriber {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Callback method được gọi khi nhận message từ Redis
     * Forward message qua WebSocket đến Frontend
     */
    public void onMessage(String message) {
        try {
            // Parse JSON từ Redis
            HelmetData data = objectMapper.readValue(message, HelmetData.class);
            
            log.info("📥 Received from Redis: {}", data.getMac());
            
            // Push qua WebSocket đến Frontend
            // Frontend subscribe: /topic/helmet/data
            messagingTemplate.convertAndSend("/topic/helmet/data", data);
            
            log.info("📤 Pushed to WebSocket: /topic/helmet/data");
            
        } catch (Exception e) {
            log.error("❌ Error processing Redis message: {}", e.getMessage(), e);
        }
    }
}
