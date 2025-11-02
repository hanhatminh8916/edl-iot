package com.hatrustsoft.bfe_foraiot.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hatrustsoft.bfe_foraiot.service.MessengerService;

import lombok.extern.slf4j.Slf4j;

/**
 * Controller để test gửi cảnh báo nguy hiểm
 */
@RestController
@RequestMapping("/api/alert")
@CrossOrigin(origins = "*")
@Slf4j
public class AlertTestController {

    private final MessengerService messengerService;

    public AlertTestController(MessengerService messengerService) {
        this.messengerService = messengerService;
    }

    /**
     * Gửi cảnh báo nguy hiểm với thông tin đầy đủ
     * POST /api/alert/send
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendAlert(@RequestBody AlertRequest request) {
        log.info("🚨 Received danger alert: {}", request);

        try {
            // Broadcast tới tất cả người đã subscribe
            messengerService.broadcastDangerAlert(
                    request.getEmployeeName(),
                    request.getAlertType(),
                    request.getLocation()
            );

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Đã gửi cảnh báo tới tất cả người dùng đã đăng ký!",
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                    "data", request
            ));
        } catch (Exception e) {
            log.error("❌ Error sending alert", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "status", "error",
                            "message", "Lỗi khi gửi cảnh báo: " + e.getMessage()
                    ));
        }
    }

    /**
     * Gửi cảnh báo với form data
     * POST /api/alert/send-form
     */
    @PostMapping("/send-form")
    public ResponseEntity<?> sendAlertForm(
            @RequestParam String employeeName,
            @RequestParam String employeeId,
            @RequestParam String position,
            @RequestParam String alertType,
            @RequestParam String location,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String description) {

        log.info("🚨 Sending alert - Employee: {}, Type: {}", employeeName, alertType);

        try {
            String fullAlertMessage = String.format(
                    "%s\nMức độ: %s\nVị trí: %s\nChức vụ: %s",
                    alertType,
                    severity != null ? severity : "Nghiêm trọng",
                    location,
                    position
            );

            if (description != null && !description.isEmpty()) {
                fullAlertMessage += "\nMô tả: " + description;
            }

            messengerService.broadcastDangerAlert(
                    employeeName + " (ID: " + employeeId + ")",
                    fullAlertMessage,
                    location
            );

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "✅ Đã gửi cảnh báo thành công!",
                    "employee", employeeName,
                    "location", location,
                    "alertType", alertType
            ));
        } catch (Exception e) {
            log.error("❌ Error sending alert", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * DTO cho Alert Request
     */
    public static class AlertRequest {
        private String employeeName;
        private String employeeId;
        private String position;
        private String alertType;
        private String location;
        private String severity;
        private String description;

        // Getters and Setters
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

        public String getEmployeeId() { return employeeId; }
        public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

        public String getPosition() { return position; }
        public void setPosition(String position) { this.position = position; }

        public String getAlertType() { return alertType; }
        public void setAlertType(String alertType) { this.alertType = alertType; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        @Override
        public String toString() {
            return "AlertRequest{" +
                    "employeeName='" + employeeName + '\'' +
                    ", employeeId='" + employeeId + '\'' +
                    ", alertType='" + alertType + '\'' +
                    ", location='" + location + '\'' +
                    '}';
        }
    }
}
