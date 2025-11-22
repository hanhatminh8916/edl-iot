package com.hatrustsoft.bfe_foraiot.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "zones")
@Data
public class Zone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;  // Zone name: Khu A, Khu B, etc.
    
    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String polygonCoordinates;  // JSON string of polygon coordinates: [[lat,lng],[lat,lng],...]
    
    private String color;  // Zone color (default: yellow)
    
    @Enumerated(EnumType.STRING)
    private ZoneStatus status;  // ACTIVE, INACTIVE
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    public enum ZoneStatus {
        ACTIVE,
        INACTIVE
    }
}
