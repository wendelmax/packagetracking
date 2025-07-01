package com.packagetracking.command.producer;

import com.packagetracking.command.dto.tracking.TrackingEventRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static com.packagetracking.command.config.RabbitMQConfig.TRACKING_EVENTS_EXCHANGE;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.modules.tracking-producer-enabled", havingValue = "true", matchIfMissing = true)
public class TrackingEventProducer {
    
    private final RabbitTemplate rabbitTemplate;
    
    /**
     * Envia evento de rastreamento para a fila RabbitMQ
     */
    public void sendTrackingEvent(TrackingEventRequest event) {
        try {
            log.info("Enviando evento de rastreamento para fila: {}", event.packageId());
            
            rabbitTemplate.convertAndSend(
                TRACKING_EVENTS_EXCHANGE,
                "tracking.events",
                event
            );
            
            log.debug("Evento de rastreamento enviado com sucesso para pacote: {}", event.packageId());
            
        } catch (Exception e) {
            log.error("Erro ao enviar evento de rastreamento para pacote {}: {}", 
                      event.packageId(), e.getMessage(), e);
            throw new RuntimeException("Erro ao enviar evento de rastreamento", e);
        }
    }
} 