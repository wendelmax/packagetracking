package com.packagetracking.command.service;

import com.packagetracking.command.dto.tracking.TrackingEventRequest;
import com.packagetracking.command.entity.TrackingEvent;
import com.packagetracking.command.repository.TrackingEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingEventServiceTest {

    @Mock
    private TrackingEventRepository trackingEventRepository;

    @InjectMocks
    private TrackingEventService trackingEventService;

    private TrackingEventRequest trackingEventRequest;
    private TrackingEvent trackingEvent;

    @BeforeEach
    void setUp() {
        trackingEventRequest = new TrackingEventRequest(
            "pacote-12345",
            "Centro de Distribuição São Paulo",
            "Pacote chegou ao centro de distribuição",
            LocalDateTime.parse("2025-01-20T11:00:00")
        );

        trackingEvent = TrackingEvent.builder()
            .id("a1b2c3d4e5f678901234567890123456")
            .packageId("pacote-12345")
            .location("Centro de Distribuição São Paulo")
            .description("Pacote chegou ao centro de distribuição")
            .date(LocalDateTime.parse("2025-01-20T11:00:00").toInstant(java.time.ZoneOffset.UTC))
            .build();
    }

    @Test
    void processTrackingEvent_Success() {
        when(trackingEventRepository.save(any(TrackingEvent.class))).thenReturn(trackingEvent);

        trackingEventService.processTrackingEvent(trackingEventRequest);

        verify(trackingEventRepository).save(any(TrackingEvent.class));
    }

    @Test
    void processTrackingEvent_WithRepositoryError_ThrowsException() {
        when(trackingEventRepository.save(any(TrackingEvent.class)))
            .thenThrow(new RuntimeException("Repository error"));

        RuntimeException exception = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            trackingEventService.processTrackingEvent(trackingEventRequest);
        });

        assertTrue(exception.getMessage().contains("Erro ao processar evento de rastreamento"));
        verify(trackingEventRepository).save(any(TrackingEvent.class));
    }

    @Test
    void processTrackingEvent_WithNullRequest_ThrowsException() {
        RuntimeException exception = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            trackingEventService.processTrackingEvent(null);
        });

        assertTrue(exception.getMessage().contains("Erro ao processar evento de rastreamento"));
    }

    private void assertTrue(boolean condition) {
        org.junit.jupiter.api.Assertions.assertTrue(condition);
    }
} 