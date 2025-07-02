package com.packagetracking.query.service;

import com.packagetracking.query.dto.PackageResponse;
import com.packagetracking.query.entity.Package;
import com.packagetracking.query.entity.PackageStatus;
import com.packagetracking.query.entity.TrackingEvent;
import com.packagetracking.query.repository.PackageRepository;
import com.packagetracking.query.repository.TrackingEventRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PackageQueryService {
    
    private final PackageRepository packageRepository;
    private final TrackingEventRepository trackingEventRepository;
    
    /**
     * Busca pacote por ID com opção de incluir eventos
     * Cache apenas para pacotes com status IN_TRANSIT
     */
    @CircuitBreaker(name = "package-cache", fallbackMethod = "getPackageFallback")
    public PackageResponse getPackage(String id, boolean includeEvents) {
        try {
            log.info("Buscando pacote: {} (incluir eventos: {})", id, includeEvents);
            
            Package packageEntity = packageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pacote não encontrado: " + id));
            
            PackageResponse response = buildPackageResponse(packageEntity, includeEvents);
            
            // Cache apenas para pacotes IN_TRANSIT
            if (packageEntity.getStatus() == PackageStatus.IN_TRANSIT) {
                log.debug("Pacote {} com status IN_TRANSIT será cacheado", id);
            }
            
            return response;
                
        } catch (Exception e) {
            log.error("Erro ao buscar pacote {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Erro ao buscar pacote", e);
        }
    }
    
    /**
     * Método com cache para pacotes IN_TRANSIT
     */
    @Cacheable(value = "packages-in-transit", key = "#id + '-' + #includeEvents", 
               condition = "#result != null and #result.status == 'IN_TRANSIT'")
    @CircuitBreaker(name = "package-cache", fallbackMethod = "getPackageFallback")
    public PackageResponse getPackageWithCache(String id, boolean includeEvents) {
        return getPackage(id, includeEvents);
    }
    
    /**
     * Fallback method para circuit breaker
     */
    public PackageResponse getPackageFallback(String id, boolean includeEvents, Exception e) {
        log.warn("Circuit breaker ativado para pacote {}: {}", id, e.getMessage());
        
        // Tenta buscar do banco sem cache
        try {
            Package packageEntity = packageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pacote não encontrado: " + id));
            
            return buildPackageResponse(packageEntity, includeEvents);
        } catch (Exception fallbackException) {
            log.error("Erro no fallback para pacote {}: {}", id, fallbackException.getMessage());
            throw new RuntimeException("Erro interno do sistema", fallbackException);
        }
    }
    

    
    /**
     * Busca lista de pacotes com filtros
     * Sem cache para listas
     */
    public List<PackageResponse> getPackages(String sender, String recipient) {
        try {
            log.info("Buscando pacotes - sender: {}, recipient: {}", sender, recipient);
            
            List<Package> packages;
            
            if (sender != null && recipient != null) {
                packages = packageRepository.findBySenderAndRecipient(sender, recipient);
            } else if (sender != null) {
                packages = packageRepository.findBySender(sender);
            } else if (recipient != null) {
                packages = packageRepository.findByRecipient(recipient);
            } else {
                packages = packageRepository.findAll();
            }
            
            return packages.stream()
                .map(packageEntity -> buildPackageResponse(packageEntity, false))
                .toList();
                
        } catch (Exception e) {
            log.error("Erro ao buscar pacotes: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao buscar pacotes", e);
        }
    }
    

    
    /**
     * Busca lista de pacotes paginada usando Spring Data JPA padrão
     */
    public Page<PackageResponse> getPackagesPaginated(String sender, String recipient, Pageable pageable) {
        try {
            log.info("Buscando pacotes paginados - sender: {}, recipient: {}, page: {}, size: {}", 
                     sender, recipient, pageable.getPageNumber(), pageable.getPageSize());
            
            // Usa a paginação padrão do Spring Data JPA
            Page<Package> packages = packageRepository.findAll(pageable);
            
            // Filtra os resultados se necessário
            if (sender != null || recipient != null) {
                List<Package> filteredPackages = packages.getContent().stream()
                    .filter(pkg -> 
                        (sender == null || pkg.getSender().equals(sender)) &&
                        (recipient == null || pkg.getRecipient().equals(recipient))
                    )
                    .collect(Collectors.toList());
                
                // Reconstrói a página com os resultados filtrados
                Page<Package> filteredPage = new PageImpl<>(filteredPackages, pageable, filteredPackages.size());
                return filteredPage.map(packageEntity -> buildPackageResponse(packageEntity, false));
            }
            
            Page<PackageResponse> responsePage = packages.map(packageEntity -> buildPackageResponse(packageEntity, false));
            
            log.debug("Pacotes paginados encontrados: {} registros", responsePage.getContent().size());
            return responsePage;
                
        } catch (Exception e) {
            log.error("Erro ao buscar pacotes paginados: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao buscar pacotes paginados", e);
        }
    }
    

    
    /**
     * Método auxiliar para construir PackageResponse
     */
    private PackageResponse buildPackageResponse(Package packageEntity, boolean includeEvents) {
        PackageResponse.PackageResponseBuilder responseBuilder = PackageResponse.builder()
            .id(packageEntity.getId())
            .description(packageEntity.getDescription())
            .sender(packageEntity.getSender())
            .recipient(packageEntity.getRecipient())
            .status(packageEntity.getStatus() != null ? packageEntity.getStatus().name() : "UNKNOWN")
            .createdAt(packageEntity.getCreatedAt())
            .updatedAt(packageEntity.getUpdatedAt())
            .deliveredAt(packageEntity.getDeliveredAt());
        
        if (includeEvents) {
            List<TrackingEvent> events = trackingEventRepository.findByPackageIdOrderByDateTimeDesc(packageEntity.getId());
            List<PackageResponse.TrackingEventResponse> eventResponses = events.stream()
                .map(event -> PackageResponse.TrackingEventResponse.builder()
                    .pacoteId(event.getPackageId())
                    .localizacao(event.getLocation())
                    .descricao(event.getDescription())
                    .dataHora(event.getDate())
                    .build())
                .collect(Collectors.toList());
            
            responseBuilder.events(eventResponses);
            log.debug("Pacote {} encontrado com {} eventos", packageEntity.getId(), eventResponses.size());
        } else {
            log.debug("Pacote {} encontrado sem eventos", packageEntity.getId());
        }
        
        return responseBuilder.build();
    }
} 