package com.hatrustsoft.bfe_foraiot.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;



@Entity
@Table(name = "helmets")
@Data
public class Helmet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Integer helmetId;

    private String macAddress;

    @ManyToOne
    @JoinColumn(name = "worker_id")
    private Worker worker;

    @Enumerated(EnumType.STRING)
    private HelmetStatus status; // ACTIVE, INACTIVE, ALERT, OFFLINE

    private Integer batteryLevel;

    private Double lastLat;
    private Double lastLon;

    private LocalDateTime lastSeen;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

