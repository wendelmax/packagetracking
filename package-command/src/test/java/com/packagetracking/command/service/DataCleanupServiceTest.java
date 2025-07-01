package com.packagetracking.command.service;

import com.packagetracking.command.entity.PackageStatus;
import com.packagetracking.command.repository.PackageJpaRepository;
import com.packagetracking.command.repository.TrackingEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataCleanupServiceTest {

    @Mock
    private PackageJpaRepository packageRepository;

    @Mock
    private TrackingEventRepository trackingEventRepository;

    @InjectMocks
    private DataCleanupService dataCleanupService;

    @Test
    void cleanupOldData_Success() {
        when(packageRepository.deleteOldPackages(any(Instant.class), eq(PackageStatus.DELIVERED.name())))
            .thenReturn(10);
        when(packageRepository.deleteOldPackages(any(Instant.class), eq(PackageStatus.CANCELLED.name())))
            .thenReturn(5);
        when(trackingEventRepository.deleteOldEvents(any(Instant.class)))
            .thenReturn(50);

        dataCleanupService.cleanupOldData();

        verify(packageRepository).deleteOldPackages(any(Instant.class), eq(PackageStatus.DELIVERED.name()));
        verify(packageRepository).deleteOldPackages(any(Instant.class), eq(PackageStatus.CANCELLED.name()));
        verify(trackingEventRepository).deleteOldEvents(any(Instant.class));
    }

    @Test
    void cleanupOldData_PackageCleanupError_ContinuesExecution() {
        when(packageRepository.deleteOldPackages(any(Instant.class), eq(PackageStatus.DELIVERED.name())))
            .thenThrow(new RuntimeException("Database error"));
        when(trackingEventRepository.deleteOldEvents(any(Instant.class)))
            .thenReturn(50);

        dataCleanupService.cleanupOldData();

        verify(packageRepository).deleteOldPackages(any(Instant.class), eq(PackageStatus.DELIVERED.name()));
        verify(packageRepository, never()).deleteOldPackages(any(Instant.class), eq(PackageStatus.CANCELLED.name()));
        verify(trackingEventRepository).deleteOldEvents(any(Instant.class));
    }

    @Test
    void cleanupOldData_EventCleanupError_ContinuesExecution() {
        when(packageRepository.deleteOldPackages(any(Instant.class), eq(PackageStatus.DELIVERED.name())))
            .thenReturn(10);
        when(packageRepository.deleteOldPackages(any(Instant.class), eq(PackageStatus.CANCELLED.name())))
            .thenReturn(5);
        when(trackingEventRepository.deleteOldEvents(any(Instant.class)))
            .thenThrow(new RuntimeException("Database error"));

        dataCleanupService.cleanupOldData();

        verify(packageRepository).deleteOldPackages(any(Instant.class), eq(PackageStatus.DELIVERED.name()));
        verify(packageRepository).deleteOldPackages(any(Instant.class), eq(PackageStatus.CANCELLED.name()));
        verify(trackingEventRepository).deleteOldEvents(any(Instant.class));
    }

    @Test
    void logDataMetrics_Success() {
        when(packageRepository.countByStatus(PackageStatus.DELIVERED.name())).thenReturn(100L);
        when(packageRepository.countByStatus(PackageStatus.IN_TRANSIT.name())).thenReturn(50L);
        when(packageRepository.countByStatus(PackageStatus.CREATED.name())).thenReturn(25L);
        when(packageRepository.countByStatus(PackageStatus.CANCELLED.name())).thenReturn(10L);

        dataCleanupService.logDataMetrics();

        verify(packageRepository).countByStatus(PackageStatus.DELIVERED.name());
        verify(packageRepository).countByStatus(PackageStatus.IN_TRANSIT.name());
        verify(packageRepository).countByStatus(PackageStatus.CREATED.name());
        verify(packageRepository).countByStatus(PackageStatus.CANCELLED.name());
    }

    @Test
    void logDataMetrics_Error_ContinuesExecution() {
        when(packageRepository.countByStatus(PackageStatus.DELIVERED.name()))
            .thenThrow(new RuntimeException("Database error"));

        dataCleanupService.logDataMetrics();

        verify(packageRepository).countByStatus(PackageStatus.DELIVERED.name());
        verify(packageRepository, never()).countByStatus(PackageStatus.IN_TRANSIT.name());
        verify(packageRepository, never()).countByStatus(PackageStatus.CREATED.name());
        verify(packageRepository, never()).countByStatus(PackageStatus.CANCELLED.name());
    }
} 