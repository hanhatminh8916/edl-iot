package com.hatrustsoft.bfe_foraiot.entity;

import java.time.LocalDateTime;
import com.hatrustsoft.bfe_foraiot.util.VietnamTimeUtils;
import com.hatrustsoft.bfe_foraiot.util.VietnamTimeUtils;
import com.hatrustsoft.bfe_foraiot.util.VietnamTimeUtils;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "safe_zones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SafeZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
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
        createdAt = VietnamTimeUtils.now();
        updatedAt = VietnamTimeUtils.now();
        if (isActive == null) {
            isActive = true;
        }
        if (color == null || color.isEmpty()) {
            color = "#3388ff";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = VietnamTimeUtils.now();
    }
}



