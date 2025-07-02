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
        String threadName = Thread.currentThread().getName();
        
        log.info("=== INÍCIO DO PROCESSAMENTO DO SERVIÇO ===");
        log.info("Processando evento de rastreamento - Pacote: {}, Thread: {}", 
                 request.packageId(), threadName);
        log.info("Dados do evento: {}", request);
        
        try {
            log.info("Criando entidade TrackingEvent para pacote: {}", request.packageId());
            
            TrackingEvent event = TrackingEvent.builder()
                .packageId(request.packageId())
                .location(request.location())
                .description(request.description())
                .date(request.date().toInstant(ZoneOffset.UTC))
                .build();
            
            event.setId(UUID.randomUUID().toString());
            
            log.info("Salvando evento no banco - ID: {}, Pacote: {}", event.getId(), event.getPackageId());
            
            TrackingEvent savedEvent = trackingEventRepository.save(event);
            
            log.info("=== EVENTO SALVO COM SUCESSO ===");
            log.info("Evento de rastreamento salvo - ID: {}, Pacote: {}, Thread: {}", 
                     savedEvent.getId(), savedEvent.getPackageId(), threadName);
            
        } catch (Exception e) {
            log.error("=== ERRO NO SERVIÇO ===");
            log.error("Erro ao processar evento de rastreamento - Pacote: {}, Thread: {}", 
                     request.packageId(), threadName);
            log.error("Mensagem de erro: {}", e.getMessage());
            log.error("Stack trace completo:", e);
            throw new RuntimeException("Erro ao processar evento de rastreamento", e);
        }
    }

    /**
     * Processa evento de rastreamento de forma assíncrona usando Virtual Threads
     */
    @Async("externalApiExecutor")
    public CompletableFuture<Void> processTrackingEventAsync(TrackingEventRequest request) {
        String threadName = Thread.currentThread().getName();
        
        log.info("=== INÍCIO DO PROCESSAMENTO ASSÍNCRONO ===");
        log.info("Processando evento de rastreamento assíncrono - Pacote: {}, Thread: {}", 
                 request.packageId(), threadName);
        
        try {
            log.info("Chamando processamento síncrono para pacote: {}", request.packageId());
            
            processTrackingEvent(request);
            
            log.info("=== PROCESSAMENTO ASSÍNCRONO CONCLUÍDO ===");
            log.info("Evento de rastreamento processado assincronamente - Pacote: {}, Thread: {}", 
                     request.packageId(), threadName);
            
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            log.error("=== ERRO NO PROCESSAMENTO ASSÍNCRONO ===");
            log.error("Erro no processamento assíncrono do evento - Pacote: {}, Thread: {}", 
                     request.packageId(), threadName);
            log.error("Mensagem de erro: {}", e.getMessage());
            log.error("Stack trace completo:", e);
            return CompletableFuture.failedFuture(e);
        }
    }
} 