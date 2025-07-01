package com.packagetracking.command.config;

import com.packagetracking.command.client.DateNagerApiClient;
import com.packagetracking.command.dto.external.Holiday;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class HolidayConfig {
    
    private final DateNagerApiClient dateNagerApiClient;
    
    public static final Map<Integer, List<Holiday>> HOLIDAY_CACHE = new ConcurrentHashMap<>();
    
    @Bean
    public CommandLineRunner preloadHolidays() {
        return args -> {
            log.info("Iniciando pré-carregamento de feriados...");
            
            int currentYear = LocalDate.now().getYear();
            
            preloadYear(currentYear);
            preloadYear(currentYear + 1);
            
            log.info("Pré-carregamento de feriados concluído. Cache: {}", HOLIDAY_CACHE.keySet());
        };
    }
    
    private void preloadYear(int year) {
        try {
            log.info("Pré-carregando feriados para o ano {}", year);
            List<Holiday> holidays = dateNagerApiClient.getPublicHolidays(year);
            
            List<Holiday> publicHolidays = holidays.stream()
                .filter(holiday -> holiday.types().contains("Public"))
                .toList();
            
            HOLIDAY_CACHE.put(year, publicHolidays);
            log.info("Pré-carregados {} feriados públicos para o ano {}", publicHolidays.size(), year);
            
        } catch (Exception e) {
            log.error("Erro ao pré-carregar feriados para o ano {}: {}", year, e.getMessage(), e);
            HOLIDAY_CACHE.put(year, List.of());
        }
    }
    
    /**
     * Método para pré-carregar um ano específico sob demanda
     */
    public static void preloadYearOnDemand(int year, DateNagerApiClient client) {
        if (HOLIDAY_CACHE.containsKey(year)) {
            log.debug("Ano {} já está em cache", year);
            return;
        }
        
        try {
            log.info("Carregando feriados para o ano {} sob demanda", year);
            List<Holiday> holidays = client.getPublicHolidays(year);
            
            List<Holiday> publicHolidays = holidays.stream()
                .filter(holiday -> holiday.types().contains("Public"))
                .toList();
            
            HOLIDAY_CACHE.put(year, publicHolidays);
            log.info("Carregados {} feriados públicos para o ano {} sob demanda", publicHolidays.size(), year);
            
        } catch (Exception e) {
            log.error("Erro ao carregar feriados para o ano {} sob demanda: {}", year, e.getMessage(), e);
            HOLIDAY_CACHE.put(year, List.of());
        }
    }
} 