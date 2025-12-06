package com.hatrustsoft.bfe_foraiot.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    @Profile("heroku")
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        
        // Database URL - Connect via ngrok tunnel to self-hosted MySQL (no query limit!)
        config.setJdbcUrl("jdbc:mysql://0.tcp.ap.ngrok.io:13542/hatrustsoft?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true&autoReconnect=true");
        config.setUsername("remote_user");
        config.setPassword("MatKhauManh123!");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // Connection pool settings - Optimized for self-hosted MySQL (no query limit!)
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(20000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);
        
        return new HikariDataSource(config);
    }
}
