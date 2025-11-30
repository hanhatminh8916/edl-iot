package com.hatrustsoft.bfe_foraiot.model;

import java.time.LocalDateTime;

import com.hatrustsoft.bfe_foraiot.entity.Employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;



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
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Enumerated(EnumType.STRING)
    private HelmetStatus status; // ACTIVE, INACTIVE, ALERT, OFFLINE

    private Integer batteryLevel;

    private Double lastLat;
    private Double lastLon;

    private LocalDateTime lastSeen;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

