package com.hatrustsoft.bfe_foraiot.config;

import java.net.URI;
import java.net.URISyntaxException;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HerokuDataSourceConfig {

    @Bean
    public DataSource dataSource() {
        String databaseUrl = System.getenv("JAWSDB_URL");
        
        if (databaseUrl != null) {
            // Parse Heroku JAWSDB_URL format: mysql://username:password@host:port/database
            try {
                URI dbUri = new URI(databaseUrl);
                
                String username = dbUri.getUserInfo().split(":")[0];
                String password = dbUri.getUserInfo().split(":")[1];
                String jdbcUrl = "jdbc:mysql://" + dbUri.getHost() + ":" + dbUri.getPort() + dbUri.getPath() 
                    + "?useSSL=true&requireSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
                
                return DataSourceBuilder
                    .create()
                    .url(jdbcUrl)
                    .username(username)
                    .password(password)
                    .driverClassName("com.mysql.cj.jdbc.Driver")
                    .build();
                    
            } catch (URISyntaxException e) {
                throw new RuntimeException("Error parsing JAWSDB_URL", e);
            }
        }
        
        // Fallback to default datasource (for local development)
        return DataSourceBuilder.create().build();
    }
}
