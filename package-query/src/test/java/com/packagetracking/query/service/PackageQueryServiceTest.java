package com.packagetracking.query.service;

import com.packagetracking.query.dto.PackageResponse;
import com.packagetracking.query.entity.Package;
import com.packagetracking.query.entity.PackageStatus;
import com.packagetracking.query.entity.TrackingEvent;
import com.packagetracking.query.repository.PackageRepository;
import com.packagetracking.query.repository.TrackingEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PackageQueryServiceTest {

    @Mock
    private PackageRepository packageRepository;

    @Mock
    private TrackingEventRepository trackingEventRepository;

    @InjectMocks
    private PackageQueryService packageQueryService;

    private Package packageEntity;
    private TrackingEvent trackingEvent;

    @BeforeEach
    void setUp() {
        packageEntity = Package.builder()
            .id("pacote-12345")
            .description("Livros para entrega")
            .sender("Loja ABC")
            .recipient("João Silva")
            .status(PackageStatus.CREATED)
            .createdAt(Instant.parse("2025-01-20T10:00:00Z"))
            .updatedAt(Instant.parse("2025-01-20T10:00:00Z"))
            .build();

        trackingEvent = new TrackingEvent();
        trackingEvent.setPackageId("pacote-12345");
        trackingEvent.setLocation("Centro de Distribuição São Paulo");
        trackingEvent.setDescription("Pacote chegou ao centro de distribuição");
        trackingEvent.setDate(LocalDateTime.parse("2025-01-20T11:00:00"));
    }

    @Test
    void getPackage_WithEvents_Success() {
        // Given
        when(packageRepository.findById("pacote-12345")).thenReturn(Optional.of(packageEntity));
        when(trackingEventRepository.findByPackageIdOrderByDateTimeDesc("pacote-12345"))
            .thenReturn(Collections.singletonList(trackingEvent));

        // When
        PackageResponse result = packageQueryService.getPackage("pacote-12345", true);

        // Then
        assertNotNull(result);
        assertEquals("pacote-12345", result.getId());
        assertEquals("Livros para entrega", result.getDescription());
        assertEquals("Loja ABC", result.getSender());
        assertEquals("João Silva", result.getRecipient());
        assertEquals("CREATED", result.getStatus());
        assertNotNull(result.getEvents());
        assertEquals(1, result.getEvents().size());
        assertEquals("pacote-12345", result.getEvents().getFirst().getPacoteId());
        assertEquals("Centro de Distribuição São Paulo", result.getEvents().getFirst().getLocalizacao());

        verify(packageRepository).findById("pacote-12345");
        verify(trackingEventRepository).findByPackageIdOrderByDateTimeDesc("pacote-12345");
    }

    @Test
    void getPackage_WithoutEvents_Success() {
        // Given
        when(packageRepository.findById("pacote-12345")).thenReturn(Optional.of(packageEntity));

        // When
        PackageResponse result = packageQueryService.getPackage("pacote-12345", false);

        // Then
        assertNotNull(result);
        assertEquals("pacote-12345", result.getId());
        assertEquals("Livros para entrega", result.getDescription());
        assertEquals("Loja ABC", result.getSender());
        assertEquals("João Silva", result.getRecipient());
        assertEquals("CREATED", result.getStatus());
        assertNull(result.getEvents());

        verify(packageRepository).findById("pacote-12345");
        verify(trackingEventRepository, never()).findByPackageIdOrderByDateTimeDesc(anyString());
    }

    @Test
    void getPackage_PackageNotFound_ThrowsException() {
        // Given
        when(packageRepository.findById("pacote-inexistente")).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> packageQueryService.getPackage("pacote-inexistente", true));

        assertEquals("Erro ao buscar pacote", exception.getMessage());
        verify(packageRepository).findById("pacote-inexistente");
    }

    @Test
    void getPackages_WithSenderFilter_Success() {
        // Given
        List<Package> packages = Collections.singletonList(packageEntity);
        when(packageRepository.findBySender("Loja ABC")).thenReturn(packages);

        // When
        List<PackageResponse> result = packageQueryService.getPackages("Loja ABC", null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("pacote-12345", result.getFirst().getId());
        assertEquals("Loja ABC", result.getFirst().getSender());

        verify(packageRepository).findBySender("Loja ABC");
        verify(packageRepository, never()).findByRecipient(anyString());
        verify(packageRepository, never()).findBySenderAndRecipient(anyString(), anyString());
        verify(packageRepository, never()).findAll();
    }

    @Test
    void getPackages_WithRecipientFilter_Success() {
        // Given
        List<Package> packages = Collections.singletonList(packageEntity);
        when(packageRepository.findByRecipient("João Silva")).thenReturn(packages);

        // When
        List<PackageResponse> result = packageQueryService.getPackages(null, "João Silva");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("pacote-12345", result.getFirst().getId());
        assertEquals("João Silva", result.getFirst().getRecipient());

        verify(packageRepository).findByRecipient("João Silva");
        verify(packageRepository, never()).findBySender(anyString());
        verify(packageRepository, never()).findBySenderAndRecipient(anyString(), anyString());
        verify(packageRepository, never()).findAll();
    }

    @Test
    void getPackages_WithBothFilters_Success() {
        // Given
        List<Package> packages = Collections.singletonList(packageEntity);
        when(packageRepository.findBySenderAndRecipient("Loja ABC", "João Silva")).thenReturn(packages);

        // When
        List<PackageResponse> result = packageQueryService.getPackages("Loja ABC", "João Silva");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("pacote-12345", result.getFirst().getId());

        verify(packageRepository).findBySenderAndRecipient("Loja ABC", "João Silva");
        verify(packageRepository, never()).findBySender(anyString());
        verify(packageRepository, never()).findByRecipient(anyString());
        verify(packageRepository, never()).findAll();
    }

    @Test
    void getPackages_WithoutFilters_Success() {
        // Given
        List<Package> packages = Collections.singletonList(packageEntity);
        when(packageRepository.findAll()).thenReturn(packages);

        // When
        List<PackageResponse> result = packageQueryService.getPackages(null, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("pacote-12345", result.getFirst().getId());

        verify(packageRepository).findAll();
        verify(packageRepository, never()).findBySender(anyString());
        verify(packageRepository, never()).findByRecipient(anyString());
        verify(packageRepository, never()).findBySenderAndRecipient(anyString(), anyString());
    }

    @Test
    void getPackage_WithNullStatus_ReturnsUnknown() {
        // Given
        Package packageWithNullStatus = Package.builder()
            .id("pacote-12345")
            .description("Livros para entrega")
            .sender("Loja ABC")
            .recipient("João Silva")
            .status(null)
            .createdAt(Instant.parse("2025-01-20T10:00:00Z"))
            .updatedAt(Instant.parse("2025-01-20T10:00:00Z"))
            .build();

        when(packageRepository.findById("pacote-12345")).thenReturn(Optional.of(packageWithNullStatus));

        // When
        PackageResponse result = packageQueryService.getPackage("pacote-12345", false);

        // Then
        assertNotNull(result);
        assertEquals("UNKNOWN", result.getStatus());
    }
}
