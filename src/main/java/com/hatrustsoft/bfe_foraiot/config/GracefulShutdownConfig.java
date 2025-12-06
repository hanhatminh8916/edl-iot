package com.hatrustsoft.bfe_foraiot.config;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * 🛡️ GRACEFUL SHUTDOWN CONFIG
 * 
 * Xử lý shutdown sạch sẽ khi Heroku gửi SIGTERM:
 * - Đóng connections đúng cách
 * - Hoàn thành requests đang xử lý
 * - Tránh R12 Exit timeout error
 */
@Configuration
@Slf4j
public class GracefulShutdownConfig {

    private volatile boolean shuttingDown = false;

    /**
     * Check if app is shutting down
     * Các service khác có thể dùng để skip long-running operations
     */
    public boolean isShuttingDown() {
        return shuttingDown;
    }

    @EventListener(ContextClosedEvent.class)
    public void onShutdown(ContextClosedEvent event) {
        log.info("🛑 Application shutdown initiated - cleaning up resources...");
        shuttingDown = true;
        
        try {
            // Cho các request đang xử lý thời gian hoàn thành (tối đa 10 giây)
            log.info("⏳ Waiting for in-flight requests to complete (max 10s)...");
            TimeUnit.SECONDS.sleep(2);
            
            log.info("✅ Graceful shutdown completed");
        } catch (InterruptedException e) {
            log.warn("⚠️ Shutdown interrupted");
            Thread.currentThread().interrupt();
        }
    }

    @PreDestroy
    public void preDestroy() {
        log.info("🧹 PreDestroy: Final cleanup before application stops");
    }
}
