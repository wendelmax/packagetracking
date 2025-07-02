package com.packagetracking.command.controller;

import com.packagetracking.command.dto.packages.PackageCancelResponse;
import com.packagetracking.command.dto.packages.PackageCreateRequest;
import com.packagetracking.command.dto.packages.PackageResponse;
import com.packagetracking.command.dto.packages.PackageUpdateRequest;
import com.packagetracking.command.service.PackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.HashMap;
import java.util.Map;

import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.resources.endpoints", havingValue = "package")
@Tag(name = "Package Command", description = "APIs para criação, atualização e cancelamento de pacotes")
public class PackageController {
    
    private final PackageService packageService;

    @Operation(
        summary = "Criar novo pacote",
        description = "Cria um novo pacote no sistema de rastreamento"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Pacote criado com sucesso",
            content = @Content(schema = @Schema(implementation = PackageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @PostMapping
    public ResponseEntity<?> createPackage(
            @Parameter(description = "Dados do pacote a ser criado", required = true)
            @Valid @RequestBody PackageCreateRequest request, 
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(e -> 
                errors.put(e.getField(), e.getDefaultMessage()));
            return ResponseEntity.badRequest().body(errors);
        }
        
        PackageResponse response = packageService.createPackageSync(request);
        
        log.info("Pacote criado com sucesso: {}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Location", "/api/packages/" + response.getId())
                .header("X-Package-ID", response.getId())
                .body(response);
    }
    
    @Operation(
        summary = "Atualizar status do pacote",
        description = "Atualiza o status de um pacote existente"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status atualizado com sucesso",
            content = @Content(schema = @Schema(implementation = PackageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "Pacote não encontrado"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updatePackageStatus(
            @Parameter(description = "ID do pacote", example = "pacote-026fbedc")
            @PathVariable String id, 
            @Parameter(description = "Novo status do pacote", required = true)
            @Valid @RequestBody PackageUpdateRequest request,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(e -> 
                errors.put(e.getField(), e.getDefaultMessage()));
            return ResponseEntity.badRequest().body(errors);
        }
        
        log.info("Atualizando status do pacote: {} para {}", id, request.getStatus());
        PackageResponse response = packageService.updatePackageStatus(id, request.getStatus());
        
        return ResponseEntity.ok()
                .header("X-Package-ID", id)
                .header("X-Status-Updated", request.getStatus())
                .body(response);
    }
    
    @Operation(
        summary = "Cancelar pacote",
        description = "Cancela um pacote existente no sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pacote cancelado com sucesso",
            content = @Content(schema = @Schema(implementation = PackageCancelResponse.class))),
        @ApiResponse(responseCode = "404", description = "Pacote não encontrado"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<PackageCancelResponse> cancelPackage(
            @Parameter(description = "ID do pacote", example = "pacote-026fbedc")
            @PathVariable String id) {
        log.info("Cancelando pacote: {}", id);
        PackageCancelResponse response = packageService.cancelPackage(id);
        
        return ResponseEntity.ok()
                .header("X-Package-ID", id)
                .header("X-Cancellation-Date", response.getDataAtualizacao().toString())
                .body(response);
    }
    

} 