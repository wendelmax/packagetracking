package com.packagetracking.command.service;

import com.packagetracking.command.entity.PackageStatus;
import com.packagetracking.command.repository.PackageJpaRepository;
import com.packagetracking.command.repository.TrackingEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataCleanupService {
    
    private final PackageJpaRepository packageRepository;
    private final TrackingEventRepository trackingEventRepository;
    
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldData() {
        log.info("Iniciando limpeza de dados antigos");
        
        Instant cutoffDate = Instant.now().minus(365, ChronoUnit.DAYS);
        
        cleanupOldPackages(cutoffDate);
        cleanupOldTrackingEvents(cutoffDate);
        
        log.info("Limpeza de dados antigos concluída");
    }
    
    private void cleanupOldPackages(Instant cutoffDate) {
        try {
            int deletedPackages = packageRepository.deleteOldPackages(cutoffDate, PackageStatus.DELIVERED.name());
            log.info("Removidos {} pacotes entregues antigos (antes de {})", deletedPackages, cutoffDate);
            
            deletedPackages = packageRepository.deleteOldPackages(cutoffDate, PackageStatus.CANCELLED.name());
            log.info("Removidos {} pacotes cancelados antigos (antes de {})", deletedPackages, cutoffDate);
        } catch (Exception e) {
            log.error("Erro ao limpar pacotes antigos", e);
        }
    }
    
    private void cleanupOldTrackingEvents(Instant cutoffDate) {
        try {
            int deletedEvents = trackingEventRepository.deleteOldEvents(cutoffDate);
            log.info("Removidos {} eventos de rastreamento antigos (antes de {})", deletedEvents, cutoffDate);
        } catch (Exception e) {
            log.error("Erro ao limpar eventos de rastreamento antigos", e);
        }
    }
    
    @Scheduled(cron = "0 0 1 * * ?")
    public void logDataMetrics() {
        try {
            long deliveredCount = packageRepository.countByStatus(PackageStatus.DELIVERED.name());
            long inTransitCount = packageRepository.countByStatus(PackageStatus.IN_TRANSIT.name());
            long createdCount = packageRepository.countByStatus(PackageStatus.CREATED.name());
            long canceledCount = packageRepository.countByStatus(PackageStatus.CANCELLED.name());
            
            log.info("Métricas de pacotes - Entregues: {}, Em trânsito: {}, Criados: {}, Cancelados: {}", 
                    deliveredCount, inTransitCount, createdCount, canceledCount);
        } catch (Exception e) {
            log.error("Erro ao coletar métricas de dados", e);
        }
    }
} 