package com.packagetracking.query.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "packages", indexes = {
    @Index(name = "idx_sender", columnList = "sender"),
    @Index(name = "idx_recipient", columnList = "recipient"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_createdAt", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Package {
    
    @Id
    private String id;
    
    @Column(nullable = false, length = 500)
    private String description;
    
    @Column(nullable = false, length = 200)
    private String sender;

    @Column(nullable = false, length = 200)
    private String recipient;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PackageStatus status;
    
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updatedAt", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "deliveredAt")
    private Instant deliveredAt;
} 