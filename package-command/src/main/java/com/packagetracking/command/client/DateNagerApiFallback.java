package com.packagetracking.command.client;

import com.packagetracking.command.config.FixedHolidayProperties;
import com.packagetracking.command.config.HolidayConfig;
import com.packagetracking.command.dto.external.Holiday;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DateNagerApiFallback implements DateNagerApiClient {
    
    private final FixedHolidayProperties fixedHolidayProperties;
    
    @Override
    public List<Holiday> getPublicHolidays(int year) {
        log.warn("Fallback ativado para feriados do ano {}. Usando feriados fixos da configuração.", year);
        
        try {
            List<Holiday> cachedHolidays = HolidayConfig.HOLIDAY_CACHE.get(year);
            if (cachedHolidays != null && !cachedHolidays.isEmpty()) {
                log.info("Usando {} feriados do cache para o ano {}", cachedHolidays.size(), year);
                return cachedHolidays;
            }
            
            String countryCode = fixedHolidayProperties.getCountryCode();
            List<Holiday> fixedHolidays = getFixedHolidaysFromConfig(countryCode, year);
            
            log.info("Usando {} feriados fixos da configuração para {} no ano {}", 
                    fixedHolidays.size(), countryCode, year);
            
            return fixedHolidays;
            
        } catch (Exception e) {
            log.error("Erro no fallback de feriados para o ano {}: {}", year, e.getMessage(), e);
            return List.of();
        }
    }
    
    private List<Holiday> getFixedHolidaysFromConfig(String countryCode, int year) {
        var fixedHolidays = fixedHolidayProperties.getFixedHolidays();
        
        if (fixedHolidays == null || fixedHolidays.length == 0) {
            log.warn("Nenhum feriado fixo configurado para o país {}", countryCode);
            return List.of();
        }
        
        return List.of(fixedHolidays).stream()
            .map(date -> new Holiday(
                year + "-" + date,
                "Feriado Fixo",
                "Fixed Holiday",
                countryCode,
                true,
                true,
                null,
                null,
                List.of("Public")
            ))
            .toList();
    }
} 