package com.packagetracking.command.dto.packages;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PackageUpdateEnrichedRequest {
    private String id;
    private String status;
    private String recipientName;
    private String recipientAddress;
    private Double weight;
    private String funFact;
    private Boolean isHoliday;
    private LocalDateTime updatedAt;
} 