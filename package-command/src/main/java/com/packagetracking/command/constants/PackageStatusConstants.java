package com.packagetracking.command.constants;

/**
 * Constantes relacionadas aos status de pacotes
 */
public final class PackageStatusConstants {
    
    private PackageStatusConstants() {
    }
    
    public static final String CREATED = "CREATED";
    public static final String IN_TRANSIT = "IN_TRANSIT";
    public static final String DELIVERED = "DELIVERED";
    public static final String CANCELLED = "CANCELLED";
    
    public static final String UPDATED = "UPDATED";
    public static final String TRACKING_UPDATED = "TRACKING_UPDATED";
    
    public static final String STATUS_REGEX = "^(CREATED|IN_TRANSIT|DELIVERED|CANCELLED)$";
    
    public static final String STATUS_VALIDATION_MESSAGE = "Status must be one of: CREATED, IN_TRANSIT, DELIVERED, CANCELLED";
} 