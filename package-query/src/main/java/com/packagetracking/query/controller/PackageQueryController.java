package com.packagetracking.query.controller;

import com.packagetracking.query.dto.PackageResponse;
import com.packagetracking.query.service.PackageQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Package Query", description = "APIs para consulta de pacotes e eventos de rastreamento")
public class PackageQueryController {
    
    private final PackageQueryService packageQueryService;
    
    /**
     * Consulta detalhes de um pacote com opção de incluir eventos de rastreamento
     * 
     * @param id ID do pacote
     * @param includeEvents true para incluir eventos, false para retornar apenas dados do pacote
     * @return Detalhes do pacote com ou sem eventos
     */
    @Operation(
        summary = "Buscar pacote por ID",
        description = "Consulta detalhes de um pacote específico com opção de incluir eventos de rastreamento"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pacote encontrado com sucesso",
            content = @Content(schema = @Schema(implementation = PackageResponse.class))),
        @ApiResponse(responseCode = "404", description = "Pacote não encontrado"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PackageResponse> getPackage(
            @Parameter(description = "ID do pacote", example = "pacote-026fbedc")
            @PathVariable String id,
            @Parameter(description = "Incluir eventos de rastreamento", example = "true")
            @RequestParam Optional<Boolean> includeEvents) {
        try {
            boolean includeEventsValue = includeEvents.orElse(true);
            log.info("Buscando detalhes do pacote: {} (incluir eventos: {})", id, includeEventsValue);
            
            // Usa o método com cache para pacotes IN_TRANSIT
            PackageResponse response = packageQueryService.getPackageWithCache(id, includeEventsValue);
            
            return ResponseEntity.ok()
                    .header("X-Package-ID", id)
                    .header("X-Include-Events", String.valueOf(includeEventsValue))
                    .header("Cache-Control", "public, max-age=300")
                    .header("ETag", "\"" + id + "-" + includeEventsValue + "\"")
                    .body(response);
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            Throwable cause = e.getCause();
            if ((msg != null && msg.contains("não encontrado")) ||
                (cause != null && cause.getMessage() != null && cause.getMessage().contains("não encontrado"))) {
                log.warn("Pacote não encontrado: {}", id);
                return ResponseEntity.notFound().build();
            }
            log.error("Erro interno ao buscar pacote {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Consulta lista de pacotes com filtros opcionais de sender e recipient
     * 
     * @param sender Filtro opcional para remetente
     * @param recipient Filtro opcional para destinatário
     * @return Lista de pacotes (sem eventos)
     */
    @Operation(
        summary = "Listar pacotes",
        description = "Consulta lista de pacotes com filtros opcionais de remetente e destinatário"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de pacotes retornada com sucesso",
            content = @Content(schema = @Schema(implementation = PackageResponse.class))),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @GetMapping
    public ResponseEntity<List<PackageResponse>> getPackages(
            @Parameter(description = "Filtro por remetente", example = "Empresa Teste")
            @RequestParam Optional<String> sender,
            @Parameter(description = "Filtro por destinatário", example = "João Silva")
            @RequestParam Optional<String> recipient) {
        try {
            log.info("Buscando lista de pacotes - sender: {}, recipient: {}", 
                     sender.orElse(null), recipient.orElse(null));
            List<PackageResponse> response = packageQueryService.getPackages(sender.orElse(null), recipient.orElse(null));
            
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(response.size()))
                    .header("X-Sender-Filter", sender.orElse("none"))
                    .header("X-Recipient-Filter", recipient.orElse("none"))
                    .header("Cache-Control", "public, max-age=180")
                    .body(response);
        } catch (RuntimeException e) {
            log.error("Erro interno ao buscar lista de pacotes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Consulta lista de pacotes paginada com filtros opcionais
     * 
     * @param sender Filtro opcional para remetente
     * @param recipient Filtro opcional para destinatário
     * @param page Número da página (padrão: 0)
     * @param size Tamanho da página (padrão: 20)
     * @return Página de pacotes (sem eventos)
     */
    @Operation(
        summary = "Listar pacotes paginados",
        description = "Consulta lista de pacotes paginada com filtros opcionais"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Página de pacotes retornada com sucesso",
            content = @Content(schema = @Schema(implementation = PackageResponse.class))),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @GetMapping("/page")
    public ResponseEntity<Page<PackageResponse>> getPackagesPaginated(
            @Parameter(description = "Filtro por remetente", example = "Empresa Teste")
            @RequestParam Optional<String> sender,
            @Parameter(description = "Filtro por destinatário", example = "João Silva")
            @RequestParam Optional<String> recipient,
            @Parameter(description = "Número da página (base 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        try {
            log.info("Buscando lista de pacotes paginada - sender: {}, recipient: {}, page: {}, size: {}", 
                     sender.orElse(null), recipient.orElse(null), page, size);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<PackageResponse> response = packageQueryService.getPackagesPaginated(sender.orElse(null), recipient.orElse(null), pageable);
            
            return ResponseEntity.ok()
                    .header("X-Total-Elements", String.valueOf(response.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(response.getTotalPages()))
                    .header("X-Current-Page", String.valueOf(response.getNumber()))
                    .header("X-Page-Size", String.valueOf(response.getSize()))
                    .header("X-Sender-Filter", sender.orElse("none"))
                    .header("X-Recipient-Filter", recipient.orElse("none"))
                    .header("Cache-Control", "public, max-age=120")
                    .body(response);
        } catch (RuntimeException e) {
            log.error("Erro interno ao buscar lista de pacotes paginada: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    

} 