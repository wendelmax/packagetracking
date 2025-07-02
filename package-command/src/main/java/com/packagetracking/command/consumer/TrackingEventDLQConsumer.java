package com.packagetracking.command.consumer;

import com.packagetracking.command.dto.tracking.TrackingEventDLQMessage;
import com.packagetracking.command.dto.tracking.TrackingEventRequest;
import com.packagetracking.command.service.DLQRetryService;
import com.packagetracking.command.service.TrackingEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class TrackingEventDLQConsumer {
    
    private final TrackingEventService trackingEventService;
    private final RabbitTemplate rabbitTemplate;
    private final DLQRetryService dlqRetryService;
    
    /**
     * Consome mensagens da DLQ e tenta reprocessá-las
     */
    @RabbitListener(queues = TRACKING_EVENTS_DLQ)
    public void processDLQMessage(TrackingEventDLQMessage dlqMessage, Message message) {
        String threadName = Thread.currentThread().getName();
        
        // Verifica se a mensagem da DLQ é válida
        if (dlqMessage == null || dlqMessage.getOriginalMessage() == null) {
            log.error("Mensagem da DLQ inválida ou malformada - Thread: {}", threadName);
            return;
        }
        
        TrackingEventRequest originalMessage = dlqMessage.getOriginalMessage();
        int retryCount = dlqRetryService.getRetryCount(message);
        
        log.info("Processando mensagem da DLQ - Pacote: {}, Tentativa: {}, Erro anterior: {}", 
                 originalMessage.packageId(), retryCount, dlqMessage.getErrorMessage());
        
        // Verifica se deve tentar reprocessar
        if (!dlqRetryService.shouldRetry(message)) {
            log.warn("Tentativas esgotadas para pacote: {}, Informações: {}", 
                     originalMessage.packageId(), dlqRetryService.getRetryInfo(message));
            dlqRetryService.markAsPermanentlyFailed(dlqMessage, retryCount);
            return;
        }
        
        try {
            // Tenta reprocessar a mensagem original
            trackingEventService.processTrackingEventAsync(originalMessage)
                .orTimeout(2000, TimeUnit.MILLISECONDS) // Timeout maior para retry
                .exceptionally(throwable -> {
                    log.error("Falha no reprocessamento da DLQ para pacote {}: {}", 
                              originalMessage.packageId(), throwable.getMessage());
                    
                    // Se falhar novamente, envia para retry com delay
                    dlqRetryService.sendToRetry(dlqMessage, retryCount);
                    return null;
                });
            
            log.info("Reprocessamento da DLQ bem-sucedido para pacote: {}", originalMessage.packageId());
            
        } catch (Exception e) {
            log.error("Erro no reprocessamento da DLQ para pacote {}: {}", 
                      originalMessage.packageId(), e.getMessage(), e);
            
            // Se falhar, envia para retry com delay
            dlqRetryService.sendToRetry(dlqMessage, retryCount);
        }
    }
} 