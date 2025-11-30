package com.hatrustsoft.bfe_foraiot.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
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

import lombok.extern.slf4j.Slf4j;

/**
 * Redis Configuration
 * - Pub/Sub cho real-time communication
 * - Cache cho helmet data
 * - Hỗ trợ SSL cho Heroku Redis (parse REDIS_URL)
 */
@Configuration
@Slf4j
public class RedisConfig {

    @Value("${REDIS_URL:#{null}}")
    private String redisUrl;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean redisSslEnabled;

    public static final String HELMET_DATA_CHANNEL = "helmet:data";

    /**
     * Redis Connection Factory với SSL support cho Heroku
     * Parse REDIS_URL nếu có (Heroku format: rediss://:password@host:port)
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        boolean useSsl = redisSslEnabled;
        
        // Parse REDIS_URL từ Heroku nếu có
        if (redisUrl != null && !redisUrl.isEmpty()) {
            try {
                log.info("🔗 Parsing REDIS_URL from environment...");
                URI uri = new URI(redisUrl);
                
                redisConfig.setHostName(uri.getHost());
                redisConfig.setPort(uri.getPort());
                
                // Password from userInfo (format: :password)
                String userInfo = uri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    String password = userInfo.split(":", 2)[1];
                    redisConfig.setPassword(password);
                }
                
                // SSL if rediss://
                useSsl = redisUrl.startsWith("rediss://");
                log.info("✅ Redis config: host={}, port={}, ssl={}", 
                    uri.getHost(), uri.getPort(), useSsl);
            } catch (Exception e) {
                log.error("❌ Failed to parse REDIS_URL: {}", e.getMessage());
            }
        } else {
            // Fallback to individual properties
            log.info("🔗 Using individual Redis properties: host={}, port={}", redisHost, redisPort);
            redisConfig.setHostName(redisHost);
            redisConfig.setPort(redisPort);
            
            if (!redisPassword.isEmpty()) {
                redisConfig.setPassword(redisPassword);
            }
        }

        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfig = 
            LettuceClientConfiguration.builder();

        if (useSsl) {
            log.info("🔒 Enabling SSL for Redis connection");
            // Disable SSL certificate validation cho Heroku Redis self-signed cert
            clientConfig.useSsl()
                .disablePeerVerification(); // Bỏ qua certificate validation
        }

        return new LettuceConnectionFactory(redisConfig, clientConfig.build());
    }

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
