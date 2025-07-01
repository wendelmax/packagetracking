package com.packagetracking.command.dto.tracking;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record TrackingEventRequest(
    @NotBlank(message = "ID do pacote é obrigatório")
    @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Package ID must contain only letters, numbers and hyphens")
    String packageId,
    
    @NotBlank(message = "Localização é obrigatória")
    @Size(min = 2, max = 200, message = "Location must be between 2 and 200 characters")
    String location,
    
    @NotBlank(message = "Descrição é obrigatória")
    @Size(min = 3, max = 500, message = "Description must be between 3 and 500 characters")
    String description,
    
    @NotNull(message = "Data do evento é obrigatória")
    @PastOrPresent(message = "A data deve ser no passado ou presente")
    @JsonProperty("date")
    LocalDateTime date
) {} 