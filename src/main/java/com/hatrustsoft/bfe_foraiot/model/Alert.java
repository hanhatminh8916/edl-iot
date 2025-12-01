package com.hatrustsoft.bfe_foraiot.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "alerts", indexes = {
    // 🚀 Index cho upsert: tìm alert theo helmet + type
    @Index(name = "idx_alerts_helmet_type", columnList = "helmet_id, alertType"),
    // 🚀 Index cho dashboard stats: đếm alerts theo thời gian
    @Index(name = "idx_alerts_triggered_at", columnList = "triggeredAt"),
    // 🚀 Index cho filter by status
    @Index(name = "idx_alerts_status", columnList = "status")
})
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

