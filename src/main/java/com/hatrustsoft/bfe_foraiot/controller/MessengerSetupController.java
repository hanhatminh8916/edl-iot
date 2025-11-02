package com.hatrustsoft.bfe_foraiot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hatrustsoft.bfe_foraiot.service.MessengerSetupService;

import lombok.extern.slf4j.Slf4j;

/**
 * Controller để setup Messenger Profile (Get Started, Greeting, Menu)
 */
@RestController
@RequestMapping("/api/messenger-setup")
@Slf4j
public class MessengerSetupController {

    private final MessengerSetupService messengerSetupService;

    public MessengerSetupController(MessengerSetupService messengerSetupService) {
        this.messengerSetupService = messengerSetupService;
    }

    /**
     * Setup tất cả: Get Started button, Greeting, Persistent Menu
     * POST /api/messenger-setup/all
     */
    @PostMapping("/all")
    public ResponseEntity<String> setupAll() {
        log.info("📞 Received request to setup all Messenger Profile settings");
        
        try {
            messengerSetupService.setupAll();
            return ResponseEntity.ok("""
                    ✅ Messenger Profile đã được cấu hình thành công!
                    
                    📋 Đã thiết lập:
                    - Get Started Button (nút "Bắt đầu")
                    - Greeting Text (lời chào)
                    - Persistent Menu (menu bên trái)
                    
                    🧪 Test:
                    1. Mở Page Ha TrustSoft trên Messenger
                    2. Bạn sẽ thấy nút "Bắt đầu"
                    3. Click vào icon ☰ (menu) để xem menu
                    """);
        } catch (Exception e) {
            log.error("❌ Error setting up Messenger Profile", e);
            return ResponseEntity.internalServerError()
                    .body("❌ Lỗi: " + e.getMessage());
        }
    }

    /**
     * Setup Get Started button
     * POST /api/messenger-setup/get-started
     */
    @PostMapping("/get-started")
    public ResponseEntity<String> setupGetStarted() {
        log.info("Setting up Get Started button");
        messengerSetupService.setupGetStartedButton();
        return ResponseEntity.ok("✅ Get Started button setup successfully!");
    }

    /**
     * Setup Greeting
     * POST /api/messenger-setup/greeting
     */
    @PostMapping("/greeting")
    public ResponseEntity<String> setupGreeting() {
        log.info("Setting up Greeting");
        messengerSetupService.setupGreeting();
        return ResponseEntity.ok("✅ Greeting setup successfully!");
    }

    /**
     * Setup Persistent Menu
     * POST /api/messenger-setup/menu
     */
    @PostMapping("/menu")
    public ResponseEntity<String> setupMenu() {
        log.info("Setting up Persistent Menu");
        messengerSetupService.setupPersistentMenu();
        return ResponseEntity.ok("✅ Persistent Menu setup successfully!");
    }

    /**
     * Xóa tất cả settings (để test lại)
     * DELETE /api/messenger-setup/all
     */
    @DeleteMapping("/all")
    public ResponseEntity<String> deleteAll() {
        log.info("Deleting all Messenger Profile settings");
        messengerSetupService.deleteAllSettings();
        return ResponseEntity.ok("✅ All settings deleted successfully!");
    }
}
