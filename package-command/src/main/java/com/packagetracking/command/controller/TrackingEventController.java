package com.packagetracking.command.controller;

import com.packagetracking.command.dto.tracking.TrackingEventRequest;
import com.packagetracking.command.producer.TrackingEventProducer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tracking-events")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.modules.tracking-producer-enabled", havingValue = "true", matchIfMissing = true)
public class TrackingEventController {
    
    private final TrackingEventProducer trackingEventProducer;

    @PostMapping
    public ResponseEntity<Void> receiveTrackingEvent(@Valid @RequestBody TrackingEventRequest request) {
        log.info("Recebido evento de rastreamento para pacote {}: {}", 
                 request.packageId(), request.description());
        
        trackingEventProducer.sendTrackingEvent(request);
        
        log.info("Evento de rastreamento enviado para fila: {}", request.packageId());
        
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .header("X-Package-ID", request.packageId())
                .header("X-Event-Description", request.description())
                .header("X-Event-Location", request.location())
                .build();
    }
} 