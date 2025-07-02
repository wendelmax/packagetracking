package com.packagetracking.command.dto.tracking;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para mensagens que falharam no processamento e foram enviadas para DLQ
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackingEventDLQMessage {
    
    private TrackingEventRequest originalMessage;
    private String errorMessage;
    private String errorType;
    private String stackTrace;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime failedAt;
    
    private String consumerThread;
    private String consumerInstance;
    
    public static TrackingEventDLQMessage fromException(TrackingEventRequest originalMessage, Exception exception, String threadName) {
        return new TrackingEventDLQMessage(
            originalMessage,
            exception.getMessage(),
            exception.getClass().getSimpleName(),
            getStackTraceAsString(exception),
            LocalDateTime.now(),
            threadName,
            "event-consumer-" + System.getProperty("server.port", "8080")
        );
    }
    
    private static String getStackTraceAsString(Exception exception) {
        StringBuilder sb = new StringBuilder();
        sb.append(exception.toString()).append("\n");
        
        for (StackTraceElement element : exception.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        
        return sb.toString();
    }
} 