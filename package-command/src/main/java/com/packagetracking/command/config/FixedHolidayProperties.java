package com.packagetracking.command.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class FixedHolidayProperties {
    
    private String countryCode;
    private String[] fixedHolidays;
} 