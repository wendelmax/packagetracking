package com.packagetracking.command.dto.external;

import java.util.List;

public record Holiday(
    String date,
    String localName,
    String name,
    String countryCode,
    boolean fixed,
    boolean global,
    List<String> counties,
    Integer launchYear,
    List<String> types
) {} 