package com.packagetracking.command.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "tracking_events", indexes = {
    @Index(name = "idx_tracking_packageId", columnList = "packageId"),
    @Index(name = "idx_tracking_date", columnList = "date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingEvent {
    
    @Id
    @Column(name = "id", length = 50)
    private String id;
    
    @Column(name = "packageId", nullable = false, length = 50)
    private String packageId;
    
    @Column(name = "location", nullable = false, length = 200)
    private String location;
    
    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "date", nullable = false, updatable = false)
    private Instant date;

} 