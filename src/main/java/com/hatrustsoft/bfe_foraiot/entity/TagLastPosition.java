package com.hatrustsoft.bfe_foraiot.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity lưu vị trí cuối cùng của tag/helmet
 * Dùng để hiển thị tag offline (màu xám) ở vị trí cuối cùng
 */
@Entity
@Table(name = "tag_last_position")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagLastPosition {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String mac; // MAC address của ESP32
    
    @Column(name = "employee_id")
    private String employeeId; // Mã nhân viên
    
    @Column(name = "employee_name")
    private String employeeName; // Tên nhân viên
    
    // Vị trí cuối cùng (meters trong hệ tọa độ 20x20)
    @Column(name = "last_x")
    private Double lastX;
    
    @Column(name = "last_y")
    private Double lastY;
    
    // Thông tin UWB distances cuối cùng
    @Column(name = "distance_a0")
    private Double distanceA0;
    
    @Column(name = "distance_a1")
    private Double distanceA1;
    
    @Column(name = "distance_a2")
    private Double distanceA2;
    
    // Battery level cuối cùng
    private Double battery;
    
    // Trạng thái online/offline
    @Column(name = "is_online")
    private Boolean isOnline;
    
    // Thời gian cập nhật cuối
    @Column(name = "last_seen")
    private LocalDateTime lastSeen;
    
    // Thời gian tạo record
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Thời gian cập nhật record
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isOnline == null) {
            isOnline = true;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
