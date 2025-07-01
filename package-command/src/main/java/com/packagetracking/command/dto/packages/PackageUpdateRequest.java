package com.packagetracking.command.dto.packages;

import com.packagetracking.command.constants.PackageStatusConstants;
import com.packagetracking.command.constants.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageUpdateRequest {
    
    @NotBlank(message = ValidationConstants.STATUS_REQUIRED)
    @Pattern(regexp = PackageStatusConstants.STATUS_REGEX, 
             message = PackageStatusConstants.STATUS_VALIDATION_MESSAGE)
    private String status;
} 