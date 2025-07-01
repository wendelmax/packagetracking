package com.packagetracking.command.dto.packages;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageCreateRequest {
    
    @NotBlank(message = "Description is required")
    @Size(min = 3, max = 500, message = "Description must be between 3 and 500 characters")
    private String description;
    
    @Size(max = 1000, message = "Fun fact must not exceed 1000 characters")
    private String funFact;
    
    @NotBlank(message = "Sender is required")
    @Size(min = 2, max = 100, message = "Sender must be between 2 and 100 characters")
    private String sender;
    
    @NotBlank(message = "Recipient is required")
    @Size(min = 2, max = 100, message = "Recipient must be between 2 and 100 characters")
    private String recipient;
            
    private Boolean isHolliday;
    
    @NotBlank(message = "Estimated delivery date is required")
    @Pattern(regexp = "\\d{2}/\\d{2}/\\d{4}", message = "Date must be in format dd/MM/yyyy")
    @JsonFormat(pattern = "dd/MM/yyyy")
    private String estimatedDeliveryDate;
} 