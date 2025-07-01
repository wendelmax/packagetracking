package com.packagetracking.command.dto.packages;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageCreateEnrichedRequest {
    
    private String id;
    private String description;
    private String funFact;
    private String sender;
    private String recipient;
    private Boolean isHolliday;
    private LocalDate estimatedDeliveryDate;
    private Instant createdAt;
    private Instant updatedAt;
} 