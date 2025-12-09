package com.hatrustsoft.bfe_foraiot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching  // 🚀 Enable Spring Cache for positioning optimization
public class BfeForAiotApplication {

    public static void main(String[] args) {
        SpringApplication.run(BfeForAiotApplication.class, args);
    }

}
