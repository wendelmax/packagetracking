package com.packagetracking.command.dto.external;

public record DogFact(
    String id,
    String type,
    DogFactAttributes attributes
) {
    
    public record DogFactAttributes(
        String body
    ) {}
} 