package com.packagetracking.command.dto.external;

import java.util.List;

public record DogFactResponse(
    List<DogFact> data
) {} 