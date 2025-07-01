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
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
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
     * Busca pacote por ID de forma assíncrona usando Virtual Threads
     */
    @Async("persistenceExecutor")
    @CircuitBreaker(name = "package-cache", fallbackMethod = "getPackageAsyncFallback")
    public CompletableFuture<PackageResponse> getPackageAsync(String id, boolean includeEvents) {
        try {
            log.debug("Buscando pacote assincronamente: {} (incluir eventos: {}) (Thread: {})", 
                     id, includeEvents, Thread.currentThread());
            
            Package packageEntity = packageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pacote não encontrado: " + id));
            
            PackageResponse response = buildPackageResponse(packageEntity, includeEvents);
            
            return CompletableFuture.completedFuture(response);
                
        } catch (Exception e) {
            log.error("Erro ao buscar pacote assincronamente {}: {}", id, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Fallback method para circuit breaker assíncrono
     */
    public CompletableFuture<PackageResponse> getPackageAsyncFallback(String id, boolean includeEvents, Exception e) {
        log.warn("Circuit breaker ativado para pacote assíncrono {}: {}", id, e.getMessage());
        
        try {
            Package packageEntity = packageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pacote não encontrado: " + id));
            
            PackageResponse response = buildPackageResponse(packageEntity, includeEvents);
            return CompletableFuture.completedFuture(response);
        } catch (Exception fallbackException) {
            log.error("Erro no fallback assíncrono para pacote {}: {}", id, fallbackException.getMessage());
            return CompletableFuture.failedFuture(new RuntimeException("Erro interno do sistema", fallbackException));
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
     * Busca lista de pacotes de forma assíncrona usando Virtual Threads
     */
    @Async("persistenceExecutor")
    public CompletableFuture<List<PackageResponse>> getPackagesAsync(String sender, String recipient) {
        try {
            log.debug("Buscando pacotes assincronamente - sender: {}, recipient: {} (Thread: {})", 
                     sender, recipient, Thread.currentThread());
            
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
            
            List<PackageResponse> responses = packages.stream()
                .map(packageEntity -> buildPackageResponse(packageEntity, false))
                .toList();
            
            log.debug("Pacotes encontrados assincronamente: {} registros (Thread: {})", 
                     responses.size(), Thread.currentThread());
            return CompletableFuture.completedFuture(responses);
                
        } catch (Exception e) {
            log.error("Erro ao buscar pacotes assincronamente: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Busca lista de pacotes paginada
     * Sem cache para paginação
     */
    public Page<PackageResponse> getPackagesPaginated(String sender, String recipient, Pageable pageable) {
        try {
            log.info("Buscando pacotes paginados - sender: {}, recipient: {}, page: {}, size: {}", 
                     sender, recipient, pageable.getPageNumber(), pageable.getPageSize());
            
            Page<Package> packages;
            
            if (sender != null && recipient != null) {
                packages = packageRepository.findBySenderAndRecipient(sender, recipient, pageable);
            } else if (sender != null) {
                packages = packageRepository.findBySender(sender, pageable);
            } else if (recipient != null) {
                packages = packageRepository.findByRecipient(recipient, pageable);
            } else {
                packages = packageRepository.findAll(pageable);
            }
            
            Page<PackageResponse> responsePage = packages.map(packageEntity -> buildPackageResponse(packageEntity, false));
            
            log.debug("Pacotes paginados encontrados: {} registros (Thread: {})", 
                     responsePage.getContent().size(), Thread.currentThread());
            return responsePage;
                
        } catch (Exception e) {
            log.error("Erro ao buscar pacotes paginados: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao buscar pacotes paginados", e);
        }
    }
    
    /**
     * Busca lista de pacotes paginada de forma assíncrona
     */
    @Async("persistenceExecutor")
    public CompletableFuture<Page<PackageResponse>> getPackagesPaginatedAsync(String sender, String recipient, Pageable pageable) {
        try {
            log.debug("Buscando pacotes paginados assincronamente - sender: {}, recipient: {}, page: {}, size: {} (Thread: {})", 
                     sender, recipient, pageable.getPageNumber(), pageable.getPageSize(), Thread.currentThread());
            
            Page<Package> packages;
            
            if (sender != null && recipient != null) {
                packages = packageRepository.findBySenderAndRecipient(sender, recipient, pageable);
            } else if (sender != null) {
                packages = packageRepository.findBySender(sender, pageable);
            } else if (recipient != null) {
                packages = packageRepository.findByRecipient(recipient, pageable);
            } else {
                packages = packageRepository.findAll(pageable);
            }
            
            Page<PackageResponse> responsePage = packages.map(packageEntity -> buildPackageResponse(packageEntity, false));
            
            log.debug("Pacotes paginados encontrados assincronamente: {} registros (Thread: {})", 
                     responsePage.getContent().size(), Thread.currentThread());
            return CompletableFuture.completedFuture(responsePage);
                
        } catch (Exception e) {
            log.error("Erro ao buscar pacotes paginados assincronamente: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
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