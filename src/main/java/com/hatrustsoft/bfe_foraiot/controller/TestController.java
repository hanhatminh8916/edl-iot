package com.hatrustsoft.bfe_foraiot.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hatrustsoft.bfe_foraiot.service.MqttMessageHandler;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/test")
@Slf4j
@CrossOrigin(origins = "*")
public class TestController {

    @Autowired
    private MqttMessageHandler mqttMessageHandler;

    /**
     * API để test MQTT message từ trang test-alerts.html
     * POST /api/test/mqtt
     */
    @PostMapping("/mqtt")
    public ResponseEntity<Map<String, Object>> testMQTTMessage(@RequestBody String payload) {
        try {
            log.info("🧪 Test MQTT message received from web: {}", payload);
            
            // Tạo Message object giống như MQTT nhận được
            Map<String, Object> headers = new HashMap<>();
            headers.put("mqtt_receivedTopic", "helmet/test");
            
            Message<String> message = new GenericMessage<>(payload, headers);
            
            // Xử lý message qua MqttMessageHandler
            mqttMessageHandler.handleMessage(message);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "MQTT message processed successfully");
            response.put("timestamp", System.currentTimeMillis());
            
            log.info("✅ Test MQTT message processed successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error processing test MQTT message: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(500).body(response);
        }
    }
}
