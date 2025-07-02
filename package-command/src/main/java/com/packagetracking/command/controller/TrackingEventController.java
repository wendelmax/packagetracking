package com.packagetracking.command.controller;

import com.packagetracking.command.dto.tracking.TrackingEventRequest;
import com.packagetracking.command.producer.TrackingEventProducer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/tracking-events")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.resources.endpoints", havingValue = "events")
@Tag(name = "Tracking Events", description = "APIs para recebimento de eventos de rastreamento")
public class TrackingEventController {
    
    private final TrackingEventProducer trackingEventProducer;

    @Operation(
        summary = "Receber evento de rastreamento",
        description = "Recebe e processa um evento de rastreamento para um pacote"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Evento aceito para processamento"),
        @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @PostMapping
    public ResponseEntity<?> receiveTrackingEvent(
            @Parameter(description = "Dados do evento de rastreamento", required = true)
            @Valid @RequestBody TrackingEventRequest request,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(e -> 
                errors.put(e.getField(), e.getDefaultMessage()));
            return ResponseEntity.badRequest().body(errors);
        }
        
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