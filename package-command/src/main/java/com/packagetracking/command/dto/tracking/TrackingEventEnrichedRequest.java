package com.packagetracking.command.dto.tracking;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TrackingEventEnrichedRequest {
    private String packageId;
    private String location;
    private String description;
    private LocalDateTime date;
} 