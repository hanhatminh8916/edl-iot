package com.hatrustsoft.bfe_foraiot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hatrustsoft.bfe_foraiot.entity.HelmetData;
import com.hatrustsoft.bfe_foraiot.service.RedisMessageSubscriber;

/**
 * Redis Configuration
 * - Pub/Sub cho real-time communication
 * - Cache cho helmet data
 */
@Configuration
public class RedisConfig {

    public static final String HELMET_DATA_CHANNEL = "helmet:data";

    /**
     * Redis Template cho publish/subscribe messages
     */
    @Bean
    public RedisTemplate<String, HelmetData> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, HelmetData> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Serializer cho key (String)
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Serializer cho value (JSON)
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Hỗ trợ LocalDateTime
        
        Jackson2JsonRedisSerializer<HelmetData> serializer = 
            new Jackson2JsonRedisSerializer<>(objectMapper, HelmetData.class);
        
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Channel topic cho helmet data
     */
    @Bean
    public ChannelTopic helmetDataTopic() {
        return new ChannelTopic(HELMET_DATA_CHANNEL);
    }

    /**
     * Message Listener Container
     * Lắng nghe messages từ Redis channel
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter messageListenerAdapter) {
        
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(messageListenerAdapter, helmetDataTopic());
        
        return container;
    }

    /**
     * Message Listener Adapter
     * Adapter để handle messages từ Redis
     */
    @Bean
    public MessageListenerAdapter messageListenerAdapter(RedisMessageSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }
}
