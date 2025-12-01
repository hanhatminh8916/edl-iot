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
import lombok.Data;

@Entity
@Table(name = "messenger_users")
@Data
public class MessengerUser {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String psid; // Page-Scoped ID từ Facebook
    
    private String firstName;
    
    private String lastName;
    
    @Column(unique = true)
    private String employeeId; // Link với Worker/Employee
    
    private Boolean subscribed = true;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime lastInteraction;
    
    @PrePersist
    protected void onCreate() {
        createdAt = VietnamTimeUtils.now();
        lastInteraction = VietnamTimeUtils.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        lastInteraction = VietnamTimeUtils.now();
    }
}



