package com.hatrustsoft.bfe_foraiot.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "helmet_data")
@Data
public class HelmetData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String mac; // MAC address của ESP32 (A48D004AEC24)
    
    @Column(name = "employee_id")
    private String employeeId; // Mã nhân viên (link với Worker)
    
    @Column(name = "employee_name")
    private String employeeName; // Tên nhân viên
    
    private Double voltage;    // 11.58V
    private Double current;    // -33.3A
    private Double power;      // 390.0W
    private Double battery;    // 100.0%
    
    private Double lat;        // Latitude GPS
    private Double lon;        // Longitude GPS
    
    private Integer counter;   // Counter từ ESP32
    
    @Column(nullable = false)
    private LocalDateTime timestamp; // Thời gian nhận dữ liệu
    
    @Column(name = "received_at")
    private LocalDateTime receivedAt; // Thời gian backend nhận
    
    @PrePersist
    protected void onCreate() {
        receivedAt = LocalDateTime.now();
    }
}
