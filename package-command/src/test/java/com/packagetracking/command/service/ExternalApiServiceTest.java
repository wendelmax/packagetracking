package com.packagetracking.command.service;

import com.packagetracking.command.client.DateNagerApiClient;
import com.packagetracking.command.client.DogApiClient;
import com.packagetracking.command.config.HolidayConfig;
import com.packagetracking.command.dto.external.DogFact;
import com.packagetracking.command.dto.external.DogFactResponse;
import com.packagetracking.command.dto.external.Holiday;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalApiServiceTest {

    @Mock
    private DateNagerApiClient dateNagerApiClient;

    @Mock
    private DogApiClient dogApiClient;

    @InjectMocks
    private ExternalApiService externalApiService;

    @BeforeEach
    void setUp() {
        // Limpa o cache antes de cada teste
        HolidayConfig.HOLIDAY_CACHE.clear();
    }

    @Test
    void isHoliday_Success() {
        LocalDate testDate = LocalDate.of(2025, 1, 1);
        List<Holiday> holidays = List.of(
            new Holiday("2025-01-01", "Ano Novo", "Ano Novo", "BR", true, true, List.of(), null, List.of("Public"))
        );

        when(dateNagerApiClient.getPublicHolidays(2025)).thenReturn(holidays);

        Mono<Boolean> result = externalApiService.isHoliday(testDate);

        result.subscribe(isHoliday -> {
            assertTrue(isHoliday);
        });

        verify(dateNagerApiClient).getPublicHolidays(2025);
    }

    @Test
    void isHoliday_NotHoliday() {
        LocalDate testDate = LocalDate.of(2025, 1, 2);
        List<Holiday> holidays = List.of(
            new Holiday("2025-01-01", "Ano Novo", "Ano Novo", "BR", true, true, List.of(), null, List.of("Public"))
        );

        when(dateNagerApiClient.getPublicHolidays(2025)).thenReturn(holidays);

        Mono<Boolean> result = externalApiService.isHoliday(testDate);

        result.subscribe(isHoliday -> {
            assertFalse(isHoliday);
        });

        verify(dateNagerApiClient).getPublicHolidays(2025);
    }

    @Test
    void isHoliday_ApiError_ReturnsFalse() {
        LocalDate testDate = LocalDate.of(2025, 1, 1);

        when(dateNagerApiClient.getPublicHolidays(2025))
            .thenThrow(new RuntimeException("API Error"));

        Mono<Boolean> result = externalApiService.isHoliday(testDate);

        result.subscribe(isHoliday -> {
            assertFalse(isHoliday);
        });

        verify(dateNagerApiClient).getPublicHolidays(2025);
    }

    @Test
    void getDogFunFact_Success() {
        DogFactResponse response = new DogFactResponse(List.of(
            new DogFact("1", "fact", new DogFact.DogFactAttributes("Dogs are amazing creatures!"))
        ));

        when(dogApiClient.getDogFacts(1)).thenReturn(response);

        Mono<String> result = externalApiService.getDogFunFact();

        result.subscribe(funFact -> {
            assertEquals("Dogs are amazing creatures!", funFact);
        });

        verify(dogApiClient).getDogFacts(1);
    }

    @Test
    void getDogFunFact_EmptyResponse_ReturnsDefault() {
        when(dogApiClient.getDogFacts(1)).thenReturn(new DogFactResponse(List.of()));

        Mono<String> result = externalApiService.getDogFunFact();

        result.subscribe(funFact -> {
            assertEquals("Cachorros são incríveis!", funFact);
        });

        verify(dogApiClient).getDogFacts(1);
    }

    @Test
    void getDogFunFact_NullResponse_ReturnsDefault() {
        when(dogApiClient.getDogFacts(1)).thenReturn(null);

        Mono<String> result = externalApiService.getDogFunFact();

        result.subscribe(funFact -> {
            assertEquals("Cachorros são incríveis!", funFact);
        });

        verify(dogApiClient).getDogFacts(1);
    }

    @Test
    void getDogFunFact_ApiError_ReturnsDefault() {
        when(dogApiClient.getDogFacts(1))
            .thenThrow(new RuntimeException("API Error"));

        Mono<String> result = externalApiService.getDogFunFact();

        result.subscribe(funFact -> {
            assertEquals("Cachorros são incríveis!", funFact);
        });

        verify(dogApiClient).getDogFacts(1);
    }

    @Test
    void isHolidayAsync_Success() {
        LocalDate testDate = LocalDate.of(2025, 1, 1);
        List<Holiday> holidays = List.of(
            new Holiday("2025-01-01", "Ano Novo", "Ano Novo", "BR", true, true, List.of(), null, List.of("Public"))
        );

        when(dateNagerApiClient.getPublicHolidays(2025)).thenReturn(holidays);

        CompletableFuture<Boolean> result = externalApiService.isHolidayAsync(testDate);

        assertTrue(result.join());
        verify(dateNagerApiClient).getPublicHolidays(2025);
    }

    @Test
    void getDogFunFactAsync_Success() {
        DogFactResponse response = new DogFactResponse(List.of(
            new DogFact("1", "fact", new DogFact.DogFactAttributes("Dogs are amazing creatures!"))
        ));

        when(dogApiClient.getDogFacts(1)).thenReturn(response);

        CompletableFuture<String> result = externalApiService.getDogFunFactAsync();

        assertEquals("Dogs are amazing creatures!", result.join());
        verify(dogApiClient).getDogFacts(1);
    }

    @Test
    void isHolidayAsync_ApiError_ReturnsFalse() {
        LocalDate testDate = LocalDate.of(2025, 1, 1);

        when(dateNagerApiClient.getPublicHolidays(2025))
            .thenThrow(new RuntimeException("API Error"));

        CompletableFuture<Boolean> result = externalApiService.isHolidayAsync(testDate);

        assertFalse(result.join());
        verify(dateNagerApiClient).getPublicHolidays(2025);
    }

    @Test
    void getDogFunFactAsync_ApiError_ReturnsDefault() {
        when(dogApiClient.getDogFacts(1))
            .thenThrow(new RuntimeException("API Error"));

        CompletableFuture<String> result = externalApiService.getDogFunFactAsync();

        assertEquals("Cachorros são incríveis!", result.join());
        verify(dogApiClient).getDogFacts(1);
    }
} 