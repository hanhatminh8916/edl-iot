package com.hatrustsoft.bfe_foraiot.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Data
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "helmet_id")
    private Helmet helmet;

    @Enumerated(EnumType.STRING)
    private AlertType alertType; // FALL, PROXIMITY, LOW_BATTERY, OUT_OF_ZONE

    @Enumerated(EnumType.STRING)
    private AlertSeverity severity; // CRITICAL, WARNING, INFO

    private String message;

    private Double gpsLat;
    private Double gpsLon;

    private LocalDateTime triggeredAt;

    @Enumerated(EnumType.STRING)
    private AlertStatus status; // PENDING, ACKNOWLEDGED, RESOLVED

    private LocalDateTime acknowledgedAt;
    private String acknowledgedBy;
}

