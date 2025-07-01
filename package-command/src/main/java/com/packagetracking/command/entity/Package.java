package com.packagetracking.command.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "packages", indexes = {
    @Index(name = "idx_package_sender", columnList = "sender"),
    @Index(name = "idx_package_recipient", columnList = "recipient"),
    @Index(name = "idx_package_createdAt", columnList = "createdAt"),
    @Index(name = "idx_package_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Package {
    
    @Id
    @Column(name = "id", length = 50)
    private String id;
    
    @Column(name = "description", nullable = false, length = 500)
    private String description;
    
    @Column(name = "funFact", length = 1000)
    private String funFact;
    
    @Column(name = "sender", nullable = false, length = 200)
    private String sender;
    
    @Column(name = "recipient", nullable = false, length = 200)
    private String recipient;
    
    @Column(name = "isHolliday", nullable = false)
    private Boolean isHolliday;
    
    @Column(name = "estimatedDeliveryDate")
    private LocalDate estimatedDeliveryDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PackageStatus status;
    
    @CreationTimestamp
    @Column(name = "createdAt", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updatedAt", nullable = false)
    private Instant updatedAt;

    @Column(name = "deliveredAt")
    private Instant deliveredAt;
} 