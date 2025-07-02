package com.packagetracking.command.service;

import com.packagetracking.command.dto.tracking.TrackingEventDLQMessage;
import com.packagetracking.command.dto.tracking.TrackingEventRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.packagetracking.command.config.RabbitMQConfig.TRACKING_EVENTS_DLQ;
import static com.packagetracking.command.config.RabbitMQConfig.TRACKING_EVENTS_EXCHANGE;

@Service
@RequiredArgsConstructor
@Slf4j
public class DLQRetryService {
    
    private final RabbitTemplate rabbitTemplate;
    
    @Value("${app.dlq.max-retry-attempts:3}")
    private int maxRetryAttempts;
    
    @Value("${app.dlq.retry-delay-minutes:5}")
    private int retryDelayMinutes;
    
    /**
     * Verifica se uma mensagem deve ser reprocessada baseado na política de retry
     */
    public boolean shouldRetry(Message message) {
        int retryCount = getRetryCount(message);
        return retryCount < maxRetryAttempts;
    }
    
    /**
     * Obtém o número de tentativas de reprocessamento
     */
    public int getRetryCount(Message message) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> deathRecords = (List<Map<String, Object>>) 
                message.getMessageProperties().getHeaders().get("x-death");
            
            if (deathRecords != null && !deathRecords.isEmpty()) {
                return deathRecords.size();
            }
        } catch (Exception e) {
            log.warn("Erro ao obter contagem de tentativas: {}", e.getMessage());
        }
        return 0;
    }
    
    /**
     * Obtém informações detalhadas sobre as tentativas de reprocessamento
     */
    public String getRetryInfo(Message message) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> deathRecords = (List<Map<String, Object>>) 
                message.getMessageProperties().getHeaders().get("x-death");
            
            if (deathRecords != null && !deathRecords.isEmpty()) {
                StringBuilder info = new StringBuilder();
                info.append("Tentativas: ").append(deathRecords.size()).append("/").append(maxRetryAttempts);
                
                for (int i = 0; i < deathRecords.size(); i++) {
                    Map<String, Object> record = deathRecords.get(i);
                    info.append("\n  Tentativa ").append(i + 1).append(": ")
                        .append("Exchange=").append(record.get("exchange"))
                        .append(", Queue=").append(record.get("queue"))
                        .append(", Reason=").append(record.get("reason"))
                        .append(", Time=").append(record.get("time"));
                }
                
                return info.toString();
            }
        } catch (Exception e) {
            log.warn("Erro ao obter informações de retry: {}", e.getMessage());
        }
        return "Informações de retry não disponíveis";
    }
    
    /**
     * Envia mensagem para retry com delay configurado
     */
    public void sendToRetry(TrackingEventDLQMessage dlqMessage, int retryCount) {
        try {
            // Adiciona informações de retry à mensagem
            dlqMessage.setFailedAt(LocalDateTime.now());
            
            // Envia para a fila de retry da DLQ
            rabbitTemplate.convertAndSend(TRACKING_EVENTS_EXCHANGE, "tracking.events.dlq.retry", dlqMessage);
            
            log.info("Mensagem enviada para retry - Pacote: {}, Tentativa: {}/{}, Delay: {} minutos", 
                     dlqMessage.getOriginalMessage().packageId(), retryCount + 1, maxRetryAttempts, retryDelayMinutes);
            
        } catch (Exception e) {
            log.error("Erro ao enviar mensagem para retry - Pacote: {}, Erro: {}", 
                      dlqMessage.getOriginalMessage().packageId(), e.getMessage(), e);
        }
    }
    
    /**
     * Marca mensagem como definitivamente falhada após esgotar tentativas
     */
    public void markAsPermanentlyFailed(TrackingEventDLQMessage dlqMessage, int retryCount) {
        log.error("Mensagem marcada como definitivamente falhada - Pacote: {}, Tentativas esgotadas: {}/{}, Erro: {}", 
                  dlqMessage.getOriginalMessage().packageId(), retryCount, maxRetryAttempts, dlqMessage.getErrorMessage());
        
        // Aqui você pode implementar notificações, alertas, etc.
        // Por exemplo, enviar para um sistema de monitoramento ou notificação
    }
} 