package com.hatrustsoft.bfe_foraiot.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "helmet_data", indexes = {
        @Index(name = "idx_helmet_timestamp", columnList = "helmet_id,timestamp")
})
@Data
public class HelmetData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "helmet_id")
    private Helmet helmet;

    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    private EventType eventType; // NORMAL, FALL, PROXIMITY, ABNORMAL

    private Double gpsLat;
    private Double gpsLon;

    private Integer batteryLevel;

    private Float uwbDistance;

    private Integer rssi;

    private String gatewayId;

    @Column(columnDefinition = "TEXT")
    private String rawData; // JSON dump
}

