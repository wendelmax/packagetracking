package com.packagetracking.query.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tracking_events", indexes = {
    @Index(name = "idx_packageId", columnList = "packageId"),
    @Index(name = "idx_date", columnList = "date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "packageId", nullable = false, length = 50)
    private String packageId;
    
    @Column(nullable = false, length = 200)
    private String location;
    
    @Column(nullable = false, length = 500)
    private String description;
    
    @Column(name = "date", nullable = false)
    private LocalDateTime date;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "packageId", insertable = false, updatable = false)
    private Package packageEntity;
} 