package com.packagetracking.command.service;

import com.packagetracking.command.client.DateNagerApiClient;
import com.packagetracking.command.client.DogApiClient;
import com.packagetracking.command.config.HolidayConfig;
import com.packagetracking.command.dto.external.Holiday;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalApiService {
    
    private final DateNagerApiClient dateNagerApiClient;
    private final DogApiClient dogApiClient;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * Verifica se uma data é feriado usando Virtual Threads
     */
    @Async("externalApiExecutor")
    public CompletableFuture<Boolean> isHolidayAsync(LocalDate date) {
        try {
            int year = date.getYear();
            String formattedDate = date.format(dateFormatter);
            
            log.debug("Verificando se {} é feriado em Virtual Thread: {}", 
                     formattedDate, Thread.currentThread());
            
            List<Holiday> holidays = HolidayConfig.HOLIDAY_CACHE.get(year);
            
            if (holidays == null) {
                log.info("Ano {} não encontrado em cache, carregando sob demanda", year);
                HolidayConfig.preloadYearOnDemand(year, dateNagerApiClient);
                holidays = HolidayConfig.HOLIDAY_CACHE.get(year);
            }
            
            boolean isHoliday = holidays.stream()
                .anyMatch(holiday -> holiday.date().equals(formattedDate));
            
            log.debug("Resultado da verificação de feriado para {}: {} (Thread: {})", 
                     formattedDate, isHoliday, Thread.currentThread());
            
            return CompletableFuture.completedFuture(isHoliday);
            
        } catch (Exception e) {
            log.error("Erro ao verificar feriado para data {}: {}", date, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * Método síncrono mantido para compatibilidade
     */
    public Mono<Boolean> isHoliday(LocalDate date) {
        return Mono.fromFuture(isHolidayAsync(date));
    }
    
    /**
     * Busca fun fact do cachorro usando Virtual Threads
     */
    @Async("externalApiExecutor")
    public CompletableFuture<String> getDogFunFactAsync() {
        try {
            log.debug("Buscando fun fact do cachorro em Virtual Thread: {}", Thread.currentThread());
            
            var response = dogApiClient.getDogFacts(1);
            
            if (response != null && response.data() != null && !response.data().isEmpty()) {
                String funFact = response.data().get(0).attributes().body();
                log.debug("Fun fact obtido: {} (Thread: {})", funFact, Thread.currentThread());
                return CompletableFuture.completedFuture(funFact);
            }
            
            log.warn("Resposta da API de cachorro não contém facts válidos");
            return CompletableFuture.completedFuture("Cachorros são incríveis!");
            
        } catch (Exception e) {
            log.error("Erro ao buscar fun fact do cachorro: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture("Cachorros são incríveis!");
        }
    }
    
    /**
     * Método síncrono mantido para compatibilidade
     */
    public Mono<String> getDogFunFact() {
        return Mono.fromFuture(getDogFunFactAsync());
    }
} 