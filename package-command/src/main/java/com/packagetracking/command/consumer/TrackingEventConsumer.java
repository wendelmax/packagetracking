package com.packagetracking.command.consumer;

import com.packagetracking.command.dto.tracking.TrackingEventRequest;
import com.packagetracking.command.service.TrackingEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static com.packagetracking.command.config.RabbitMQConfig.TRACKING_EVENTS_QUEUE;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.modules.tracking-consumer-enabled", havingValue = "true", matchIfMissing = true)
public class TrackingEventConsumer {
    
    private final TrackingEventService trackingEventService;
    
    /**
     * Consome eventos de rastreamento da fila RabbitMQ com processamento assíncrono
     */
    @RabbitListener(queues = TRACKING_EVENTS_QUEUE)
    public void processTrackingEvent(TrackingEventRequest event) {
        try {
            log.info("Recebido evento de rastreamento para pacote: {} (Thread: {})", 
                     event.packageId(), Thread.currentThread());
            
            trackingEventService.processTrackingEventAsync(event)
                .orTimeout(1000, TimeUnit.MILLISECONDS)
                .exceptionally(throwable -> {
                    log.error("Timeout ou erro no processamento do evento para pacote {}: {}", 
                              event.packageId(), throwable.getMessage());
                    return null;
                });
            
        } catch (Exception e) {
            log.error("Erro ao processar evento de rastreamento para pacote {}: {}", 
                      event.packageId(), e.getMessage(), e);
            
            throw new RuntimeException("Erro ao processar evento de rastreamento", e);
        }
    }
} 