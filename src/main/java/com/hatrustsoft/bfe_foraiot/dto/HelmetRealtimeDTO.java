package com.hatrustsoft.bfe_foraiot.dto;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for realtime helmet data via WebSocket
 * Includes UWB distances for 2D positioning (not stored in DB)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HelmetRealtimeDTO {
    
    private String mac;
    private String employeeId;
    private String employeeName;
    
    private Double battery;
    private Double voltage;
    private Double current;
    private Double temp;        // Body temperature
    private Double heartRate;   // HR (BPM)
    private Double spo2;        // SpO2 %
    
    private Double lat;
    private Double lon;
    
    // UWB Data for 2D Positioning (NOT stored in DB)
    private Map<String, Double> uwb;  // {"A0": 4.25, "A1": 4.43, "A2": 4.35, "baseline_A1": 1.8, "baseline_A2": 0.49}
    private Boolean uwbReady;         // true if UWB positioning is ready
    
    // Safety alerts
    private Boolean fallDetected;
    private Boolean helpRequest;
    
    // Status
    private String status;      // "online", "offline", "warning"
    private LocalDateTime timestamp;
    private LocalDateTime receivedAt;
}
