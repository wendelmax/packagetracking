package com.packagetracking.command.service;

import com.packagetracking.command.dto.tracking.TrackingEventRequest;
import com.packagetracking.command.entity.TrackingEvent;
import com.packagetracking.command.repository.TrackingEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingEventService {
    
    private final TrackingEventRepository trackingEventRepository;

    /**
     * Processa evento de rastreamento recebido da fila RabbitMQ
     */
    @Transactional
    public void processTrackingEvent(TrackingEventRequest request) {
        try {
            log.debug("Processando evento de rastreamento: {} (Thread: {})", 
                     request, Thread.currentThread());
            
            TrackingEvent event = TrackingEvent.builder()
                .packageId(request.packageId())
                .location(request.location())
                .description(request.description())
                .date(request.date().toInstant(ZoneOffset.UTC))
                .build();
            
            event.setId(UUID.randomUUID().toString());
            
            TrackingEvent savedEvent = trackingEventRepository.save(event);
            
            log.info("Evento de rastreamento salvo com ID: {} para pacote: {} (Thread: {})", 
                     savedEvent.getId(), savedEvent.getPackageId(), Thread.currentThread());
            
        } catch (Exception e) {
            log.error("Erro ao processar evento de rastreamento: {} (Thread: {})", 
                     e.getMessage(), Thread.currentThread(), e);
            throw new RuntimeException("Erro ao processar evento de rastreamento", e);
        }
    }

    /**
     * Processa evento de rastreamento de forma assíncrona usando Virtual Threads
     */
    @Async("externalApiExecutor")
    public CompletableFuture<Void> processTrackingEventAsync(TrackingEventRequest request) {
        try {
            log.debug("Processando evento de rastreamento assíncrono: {} (Thread: {})", 
                     request, Thread.currentThread());
            
            processTrackingEvent(request);
            
            log.info("Evento de rastreamento processado assincronamente para pacote: {} (Thread: {})", 
                     request.packageId(), Thread.currentThread());
            
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            log.error("Erro no processamento assíncrono do evento: {} (Thread: {})", 
                     e.getMessage(), Thread.currentThread(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
} 