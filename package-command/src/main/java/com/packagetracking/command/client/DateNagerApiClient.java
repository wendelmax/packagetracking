package com.packagetracking.command.client;

import com.packagetracking.command.dto.external.Holiday;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(
    name = "holiday-api",
    url = "${external.apis.holiday.base-url}",
    fallback = DateNagerApiFallback.class
)
public interface DateNagerApiClient {
    
    @GetMapping("/api/v3/PublicHolidays/{year}/BR")
    List<Holiday> getPublicHolidays(@PathVariable int year);
    
    class DateNagerApiFallback implements DateNagerApiClient {
        @Override
        public List<Holiday> getPublicHolidays(int year) {

            return List.of();
        }
    }
} 