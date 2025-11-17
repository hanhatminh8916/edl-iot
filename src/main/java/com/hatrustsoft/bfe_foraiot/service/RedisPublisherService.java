package com.hatrustsoft.bfe_foraiot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

import com.hatrustsoft.bfe_foraiot.entity.HelmetData;

import lombok.extern.slf4j.Slf4j;

/**
 * Redis Publisher Service
 * Publish helmet data to Redis channel
 */
@Service
@Slf4j
public class RedisPublisherService {

    @Autowired
    private RedisTemplate<String, HelmetData> redisTemplate;

    @Autowired
    private ChannelTopic helmetDataTopic;

    /**
     * Publish helmet data to Redis channel
     * Sẽ được subscribe bởi RedisMessageSubscriber
     */
    public void publishHelmetData(HelmetData data) {
        try {
            redisTemplate.convertAndSend(helmetDataTopic.getTopic(), data);
            log.debug("📡 Published to Redis: {}", data.getMac());
        } catch (Exception e) {
            log.error("❌ Error publishing to Redis: {}", e.getMessage(), e);
        }
    }
}
