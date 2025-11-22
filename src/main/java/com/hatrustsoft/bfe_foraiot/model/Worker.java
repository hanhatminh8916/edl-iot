package com.hatrustsoft.bfe_foraiot.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "workers")
@Data
public class Worker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    private String employeeId;
    
    private String position;  // Job title: Công nhân, Kỹ sư, etc.
    
    private String location;  // Work location/area: Khu đông, Bắc, etc.
    
    private String department;
    
    private String phoneNumber;
    
    private String email;
    
    @Enumerated(EnumType.STRING)
    private WorkerStatus status; // ACTIVE, INACTIVE, ON_LEAVE
    
    private LocalDateTime hiredDate;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    public enum WorkerStatus {
        ACTIVE,
        INACTIVE,
        ON_LEAVE
    }
}
