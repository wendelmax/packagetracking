package com.packagetracking.command.consumer;

import com.packagetracking.command.dto.tracking.TrackingEventDLQMessage;
import com.packagetracking.command.dto.tracking.TrackingEventRequest;
import com.packagetracking.command.service.TrackingEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static com.packagetracking.command.config.RabbitMQConfig.TRACKING_EVENTS_DLQ;
import static com.packagetracking.command.config.RabbitMQConfig.TRACKING_EVENTS_EXCHANGE;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.resources.endpoints", havingValue = "none")
public class TrackingEventConsumer {
    
    private final TrackingEventService trackingEventService;
    private final RabbitTemplate rabbitTemplate;
    
    /**
     * Consome eventos de rastreamento da fila RabbitMQ com processamento assíncrono
     */
    @RabbitListener(queues = "tracking.events.queue")
    public void processTrackingEvent(TrackingEventRequest event, Message message) {
        String threadName = Thread.currentThread().getName();
        String messageId = message.getMessageProperties().getMessageId();
        
        log.info("=== INÍCIO DO PROCESSAMENTO ===");
        log.info("Recebido evento de rastreamento - Pacote: {}, Thread: {}, MessageId: {}", 
                 event.packageId(), threadName, messageId);
        log.info("Dados do evento: {}", event);
        
        try {
            log.info("Iniciando processamento assíncrono para pacote: {}", event.packageId());
            
            trackingEventService.processTrackingEventAsync(event)
                .orTimeout(1000, TimeUnit.MILLISECONDS)
                .exceptionally(throwable -> {
                    log.error("=== ERRO NO PROCESSAMENTO ASSÍNCRONO ===");
                    log.error("Timeout ou erro no processamento do evento para pacote {}: {}", 
                              event.packageId(), throwable.getMessage());
                    log.error("Stack trace completo:", throwable);
                    
                    // Envia para DLQ com informações detalhadas do erro
                    sendToDLQ(event, new RuntimeException("Timeout ou erro no processamento assíncrono: " + throwable.getMessage(), throwable), threadName);
                    return null;
                })
                .thenAccept(result -> {
                    if (result != null) {
                        log.info("=== PROCESSAMENTO CONCLUÍDO COM SUCESSO ===");
                        log.info("Evento processado com sucesso para pacote: {}", event.packageId());
                    } else {
                        log.warn("=== PROCESSAMENTO RETORNOU NULL ===");
                        log.warn("Resultado null para pacote: {}", event.packageId());
                    }
                });
            
        } catch (Exception e) {
            log.error("=== ERRO CRÍTICO NO PROCESSAMENTO ===");
            log.error("Erro ao processar evento de rastreamento para pacote {}: {}", 
                      event.packageId(), e.getMessage());
            log.error("Stack trace completo:", e);
            
            // Envia para DLQ com informações detalhadas do erro
            sendToDLQ(event, e, threadName);
            
            // Rejeita a mensagem para que não seja reprocessada
            throw new AmqpRejectAndDontRequeueException("Erro ao processar evento de rastreamento", e);
        }
        
        log.info("=== FIM DO PROCESSAMENTO ===");
    }
    
    /**
     * Envia mensagem enriquecida para a DLQ com informações detalhadas do erro
     */
    private void sendToDLQ(TrackingEventRequest originalMessage, Exception exception, String threadName) {
        try {
            log.info("=== ENVIANDO PARA DLQ ===");
            log.info("Pacote: {}, Erro: {}, Tipo: {}", 
                     originalMessage.packageId(), exception.getMessage(), exception.getClass().getSimpleName());
            
            TrackingEventDLQMessage dlqMessage = TrackingEventDLQMessage.fromException(originalMessage, exception, threadName);
            
            rabbitTemplate.convertAndSend(TRACKING_EVENTS_EXCHANGE, "tracking.events.dlq", dlqMessage);
            
            log.warn("Mensagem enviada para DLQ - Pacote: {}, Erro: {}, Tipo: {}", 
                     originalMessage.packageId(), exception.getMessage(), exception.getClass().getSimpleName());
            
        } catch (Exception dlqException) {
            log.error("=== ERRO AO ENVIAR PARA DLQ ===");
            log.error("Erro ao enviar mensagem para DLQ - Pacote: {}, Erro original: {}, Erro DLQ: {}", 
                      originalMessage.packageId(), exception.getMessage(), dlqException.getMessage(), dlqException);
        }
    }
} 