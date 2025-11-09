package com.hatrustsoft.bfe_foraiot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "safe_zones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SafeZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String zoneName; // Tên khu vực (vd: "Khu vực sản xuất A")

    @Column(nullable = false, columnDefinition = "TEXT")
    private String polygonCoordinates; // JSON array: [[lat1,lon1],[lat2,lon2],...]

    @Column(nullable = false)
    private String color; // Màu vẽ polygon (vd: "#3388ff")

    @Column(nullable = false)
    private Boolean isActive; // Khu vực đang active hay không

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy; // User nào tạo

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
        if (color == null || color.isEmpty()) {
            color = "#3388ff";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
