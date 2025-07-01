package com.packagetracking.command.client;

import com.packagetracking.command.dto.external.DogFactResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "dog-api",
    url = "${external.apis.dog.base-url}",
    fallback = DogApiFallback.class
)
public interface DogApiClient {
    
    @GetMapping("/api/v2/facts")
    DogFactResponse getDogFacts(@RequestParam(value = "limit", required = false) Integer limit);
} 