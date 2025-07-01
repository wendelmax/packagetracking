package com.packagetracking.command.service;

import com.packagetracking.command.dto.packages.PackageCreateEnrichedRequest;
import com.packagetracking.command.dto.packages.PackageCreateRequest;
import com.packagetracking.command.dto.packages.PackageResponse;
import com.packagetracking.command.entity.Package;
import com.packagetracking.command.entity.PackageStatus;
import com.packagetracking.command.repository.PackageJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PackageServiceTest {

    @Mock
    private PackageJpaRepository packageRepository;

    @Mock
    private ExternalApiService externalApiService;

    @InjectMocks
    private PackageService packageService;

    private PackageCreateRequest createRequest;
    private PackageCreateEnrichedRequest enrichedRequest;
    private Package packageEntity;
    private PackageResponse packageResponse;

    @BeforeEach
    void setUp() {
        createRequest = PackageCreateRequest.builder()
            .description("Livros para entrega")
            .funFact("Fato interessante")
            .sender("Loja ABC")
            .recipient("João Silva")
            .isHolliday(false)
            .estimatedDeliveryDate("25/01/2025")
            .build();

        enrichedRequest = PackageCreateEnrichedRequest.builder()
            .id("pacote-12345")
            .description("Livros para entrega")
            .funFact("Fato interessante sobre cães")
            .sender("Loja ABC")
            .recipient("João Silva")
            .isHolliday(false)
            .estimatedDeliveryDate(LocalDate.of(2025, 1, 25))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        packageEntity = Package.builder()
            .id("pacote-12345")
            .description("Livros para entrega")
            .funFact("Fato interessante sobre cães")
            .sender("Loja ABC")
            .recipient("João Silva")
            .isHolliday(false)
            .estimatedDeliveryDate(LocalDate.of(2025, 1, 25))
            .status(PackageStatus.CREATED)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        packageResponse = PackageResponse.builder()
            .id("pacote-12345")
            .description("Livros para entrega")
            .sender("Loja ABC")
            .recipient("João Silva")
            .status("CREATED")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    @Test
    void createPackageSync_Success() {
        when(externalApiService.isHoliday(any(LocalDate.class))).thenReturn(Mono.just(false));
        when(externalApiService.getDogFunFact()).thenReturn(Mono.just("Fato interessante sobre cães"));
        when(packageRepository.save(any(Package.class))).thenReturn(packageEntity);

        PackageResponse result = packageService.createPackageSync(createRequest);

        assertNotNull(result);
        assertEquals("CREATED", result.getStatus());
        verify(packageRepository).save(any(Package.class));
        verify(externalApiService).isHoliday(any(LocalDate.class));
        verify(externalApiService).getDogFunFact();
    }

    @Test
    void createPackageSync_WithNullRequest_ThrowsException() {
        when(externalApiService.isHoliday(any(LocalDate.class))).thenReturn(Mono.just(false));
        when(externalApiService.getDogFunFact()).thenReturn(Mono.just("Fato interessante sobre cães"));
        
        assertThrows(RuntimeException.class, () -> {
            packageService.createPackageSync(null);
        });
    }

    @Test
    void updatePackageStatus_SuccessFromCreatedToInTransit() {
        Package existingPackage = Package.builder()
            .id("pacote-12345")
            .description("Livros para entrega")
            .sender("Loja ABC")
            .recipient("João Silva")
            .status(PackageStatus.CREATED)
            .isHolliday(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        Package updatedPackage = Package.builder()
            .id("pacote-12345")
            .description("Livros para entrega")
            .sender("Loja ABC")
            .recipient("João Silva")
            .status(PackageStatus.IN_TRANSIT)
            .isHolliday(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(packageRepository.findById("pacote-12345")).thenReturn(java.util.Optional.of(existingPackage));
        when(packageRepository.save(any(Package.class))).thenReturn(updatedPackage);

        PackageResponse result = packageService.updatePackageStatus("pacote-12345", "IN_TRANSIT");

        assertNotNull(result);
        assertEquals("IN_TRANSIT", result.getStatus());
        verify(packageRepository).save(any(Package.class));
    }

    @Test
    void updatePackageStatus_SuccessFromInTransitToDelivered() {
        Package existingPackage = Package.builder()
            .id("pacote-12345")
            .description("Livros para entrega")
            .sender("Loja ABC")
            .recipient("João Silva")
            .status(PackageStatus.IN_TRANSIT)
            .isHolliday(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        Package updatedPackage = Package.builder()
            .id("pacote-12345")
            .description("Livros para entrega")
            .sender("Loja ABC")
            .recipient("João Silva")
            .status(PackageStatus.DELIVERED)
            .isHolliday(true)
            .deliveredAt(Instant.now())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(packageRepository.findById("pacote-12345")).thenReturn(java.util.Optional.of(existingPackage));
        when(packageRepository.save(any(Package.class))).thenReturn(updatedPackage);

        PackageResponse result = packageService.updatePackageStatus("pacote-12345", "DELIVERED");

        assertNotNull(result);
        assertEquals("DELIVERED", result.getStatus());
        assertNotNull(result.getDeliveredAt());
        verify(packageRepository).save(any(Package.class));
    }

    @Test
    void updatePackageStatus_InvalidTransition_ThrowsException() {
        Package existingPackage = Package.builder()
            .id("pacote-12345")
            .description("Livros para entrega")
            .sender("Loja ABC")
            .recipient("João Silva")
            .status(PackageStatus.CREATED)
            .isHolliday(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(packageRepository.findById("pacote-12345")).thenReturn(java.util.Optional.of(existingPackage));

        assertThrows(RuntimeException.class, () -> {
            packageService.updatePackageStatus("pacote-12345", "DELIVERED");
        });

        verify(packageRepository, never()).save(any(Package.class));
    }

    @Test
    void updatePackageStatus_PackageNotFound_ThrowsException() {
        when(packageRepository.findById("pacote-inexistente")).thenReturn(java.util.Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            packageService.updatePackageStatus("pacote-inexistente", "IN_TRANSIT");
        });

        verify(packageRepository, never()).save(any(Package.class));
    }

    @Test
    void updatePackageStatus_InvalidStatus_ThrowsException() {
        Package existingPackage = Package.builder()
            .id("pacote-12345")
            .description("Livros para entrega")
            .sender("Loja ABC")
            .recipient("João Silva")
            .status(PackageStatus.CREATED)
            .isHolliday(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(packageRepository.findById("pacote-12345")).thenReturn(java.util.Optional.of(existingPackage));

        assertThrows(RuntimeException.class, () -> {
            packageService.updatePackageStatus("pacote-12345", "STATUS_INVALIDO");
        });

        verify(packageRepository, never()).save(any(Package.class));
    }

    @Test
    void cancelPackage_Success() {
        Package existingPackage = Package.builder()
            .id("pacote-12345")
            .description("Livros para entrega")
            .sender("Loja ABC")
            .recipient("João Silva")
            .status(PackageStatus.CREATED)
            .isHolliday(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        Package cancelledPackage = Package.builder()
            .id("pacote-12345")
            .description("Livros para entrega")
            .sender("Loja ABC")
            .recipient("João Silva")
            .status(PackageStatus.CANCELLED)
            .isHolliday(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(packageRepository.findById("pacote-12345")).thenReturn(java.util.Optional.of(existingPackage));
        when(packageRepository.save(any(Package.class))).thenReturn(cancelledPackage);

        var result = packageService.cancelPackage("pacote-12345");

        assertNotNull(result);
        assertEquals("CANCELLED", result.getStatus());
        assertEquals("pacote-12345", result.getId());
        assertNotNull(result.getDataAtualizacao());
        verify(packageRepository).save(any(Package.class));
    }

    @Test
    void cancelPackage_PackageNotFound_ThrowsException() {
        when(packageRepository.findById("pacote-inexistente")).thenReturn(java.util.Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            packageService.cancelPackage("pacote-inexistente");
        });

        verify(packageRepository, never()).save(any(Package.class));
    }

    @Test
    void cancelPackage_AlreadyInTransit_ThrowsException() {
        Package existingPackage = Package.builder()
            .id("pacote-12345")
            .description("Livros para entrega")
            .sender("Loja ABC")
            .recipient("João Silva")
            .status(PackageStatus.IN_TRANSIT)
            .isHolliday(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(packageRepository.findById("pacote-12345")).thenReturn(java.util.Optional.of(existingPackage));

        assertThrows(RuntimeException.class, () -> {
            packageService.cancelPackage("pacote-12345");
        });

        verify(packageRepository, never()).save(any(Package.class));
    }

    @Test
    void cancelPackage_AlreadyDelivered_ThrowsException() {
        Package existingPackage = Package.builder()
            .id("pacote-12345")
            .description("Livros para entrega")
            .sender("Loja ABC")
            .recipient("João Silva")
            .status(PackageStatus.DELIVERED)
            .isHolliday(true)
            .deliveredAt(Instant.now())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(packageRepository.findById("pacote-12345")).thenReturn(java.util.Optional.of(existingPackage));

        assertThrows(RuntimeException.class, () -> {
            packageService.cancelPackage("pacote-12345");
        });

        verify(packageRepository, never()).save(any(Package.class));
    }

    @Test
    void cancelPackage_AlreadyCancelled_ThrowsException() {
        Package existingPackage = Package.builder()
            .id("pacote-12345")
            .description("Livros para entrega")
            .sender("Loja ABC")
            .recipient("João Silva")
            .status(PackageStatus.CANCELLED)
            .isHolliday(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(packageRepository.findById("pacote-12345")).thenReturn(java.util.Optional.of(existingPackage));

        assertThrows(RuntimeException.class, () -> {
            packageService.cancelPackage("pacote-12345");
        });

        verify(packageRepository, never()).save(any(Package.class));
    }
} 