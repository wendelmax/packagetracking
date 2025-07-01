package com.packagetracking.query.controller;

import com.packagetracking.query.dto.PackageResponse;
import com.packagetracking.query.service.PackageQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PackageQueryControllerTest {

    @Mock
    private PackageQueryService packageQueryService;

    @InjectMocks
    private PackageQueryController packageQueryController;

    private PackageResponse packageResponse;
    private PackageResponse.TrackingEventResponse eventResponse;
    private Page<PackageResponse> packagePage;

    @BeforeEach
    void setUp() {
        eventResponse = PackageResponse.TrackingEventResponse.builder()
            .pacoteId("pacote-12345")
            .localizacao("Centro de Distribuição São Paulo")
            .descricao("Pacote chegou ao centro de distribuição")
            .dataHora(LocalDateTime.parse("2025-01-20T11:00:00"))
            .build();

        packageResponse = PackageResponse.builder()
            .id("pacote-12345")
            .description("Livros para entrega")
            .sender("Loja ABC")
            .recipient("João Silva")
            .status("CREATED")
            .createdAt(Instant.parse("2025-01-20T10:00:00Z"))
            .updatedAt(Instant.parse("2025-01-20T10:00:00Z"))
            .events(Arrays.asList(eventResponse))
            .build();

        Pageable pageable = PageRequest.of(0, 20);
        packagePage = new PageImpl<>(Arrays.asList(packageResponse), pageable, 1);
    }

    @Test
    void getPackage_WithEvents_Success() {
        // Given
        when(packageQueryService.getPackageWithCache("pacote-12345", true)).thenReturn(packageResponse);

        // When
        ResponseEntity<PackageResponse> response = packageQueryController.getPackage("pacote-12345", Optional.of(true));

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("pacote-12345", response.getBody().getId());
        assertEquals("Livros para entrega", response.getBody().getDescription());
        assertEquals("Loja ABC", response.getBody().getSender());
        assertEquals("João Silva", response.getBody().getRecipient());
        assertEquals("CREATED", response.getBody().getStatus());
        assertNotNull(response.getBody().getEvents());
        assertEquals(1, response.getBody().getEvents().size());

        verify(packageQueryService).getPackageWithCache("pacote-12345", true);
    }

    @Test
    void getPackage_WithoutEvents_Success() {
        // Given
        PackageResponse packageWithoutEvents = PackageResponse.builder()
            .id("pacote-12345")
            .description("Livros para entrega")
            .sender("Loja ABC")
            .recipient("João Silva")
            .status("CREATED")
            .createdAt(Instant.parse("2025-01-20T10:00:00Z"))
            .updatedAt(Instant.parse("2025-01-20T10:00:00Z"))
            .build();

        when(packageQueryService.getPackageWithCache("pacote-12345", false)).thenReturn(packageWithoutEvents);

        // When
        ResponseEntity<PackageResponse> response = packageQueryController.getPackage("pacote-12345", Optional.of(false));

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("pacote-12345", response.getBody().getId());
        assertNull(response.getBody().getEvents());

        verify(packageQueryService).getPackageWithCache("pacote-12345", false);
    }

    @Test
    void getPackage_DefaultIncludeEvents_Success() {
        // Given
        when(packageQueryService.getPackageWithCache("pacote-12345", true)).thenReturn(packageResponse);

        // When
        ResponseEntity<PackageResponse> response = packageQueryController.getPackage("pacote-12345", Optional.empty());

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("pacote-12345", response.getBody().getId());

        verify(packageQueryService).getPackageWithCache("pacote-12345", true);
    }

    @Test
    void getPackage_PackageNotFound_ReturnsNotFound() {
        // Given
        when(packageQueryService.getPackageWithCache("pacote-inexistente", true))
            .thenThrow(new RuntimeException("Pacote não encontrado: pacote-inexistente"));

        // When
        ResponseEntity<PackageResponse> response = packageQueryController.getPackage("pacote-inexistente", Optional.of(true));

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());

        verify(packageQueryService).getPackageWithCache("pacote-inexistente", true);
    }

    @Test
    void getPackageAsync_WithEvents_Success() throws ExecutionException, InterruptedException {
        // Given
        CompletableFuture<PackageResponse> future = CompletableFuture.completedFuture(packageResponse);
        when(packageQueryService.getPackageAsync("pacote-12345", true)).thenReturn(future);

        // When
        CompletableFuture<ResponseEntity<PackageResponse>> responseFuture = 
            packageQueryController.getPackageAsync("pacote-12345", Optional.of(true));
        ResponseEntity<PackageResponse> response = responseFuture.get();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("pacote-12345", response.getBody().getId());

        verify(packageQueryService).getPackageAsync("pacote-12345", true);
    }

    @Test
    void getPackageAsync_PackageNotFound_ReturnsNotFound() throws ExecutionException, InterruptedException {
        // Given
        CompletableFuture<PackageResponse> future = CompletableFuture.failedFuture(new RuntimeException("Pacote não encontrado: pacote-inexistente"));
        when(packageQueryService.getPackageAsync("pacote-inexistente", true)).thenReturn(future);

        // When
        CompletableFuture<ResponseEntity<PackageResponse>> responseFuture = 
            packageQueryController.getPackageAsync("pacote-inexistente", Optional.of(true));
        ResponseEntity<PackageResponse> response = responseFuture.get();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());

        verify(packageQueryService).getPackageAsync("pacote-inexistente", true);
    }

    @Test
    void getPackages_WithSenderFilter_Success() {
        // Given
        List<PackageResponse> packages = Arrays.asList(packageResponse);
        when(packageQueryService.getPackages("Loja ABC", null)).thenReturn(packages);

        // When
        ResponseEntity<List<PackageResponse>> response = packageQueryController.getPackages(Optional.of("Loja ABC"), Optional.empty());

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("pacote-12345", response.getBody().get(0).getId());

        verify(packageQueryService).getPackages("Loja ABC", null);
    }

    @Test
    void getPackages_WithRecipientFilter_Success() {
        // Given
        List<PackageResponse> packages = Arrays.asList(packageResponse);
        when(packageQueryService.getPackages(null, "João Silva")).thenReturn(packages);

        // When
        ResponseEntity<List<PackageResponse>> response = packageQueryController.getPackages(Optional.empty(), Optional.of("João Silva"));

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("pacote-12345", response.getBody().get(0).getId());

        verify(packageQueryService).getPackages(null, "João Silva");
    }

    @Test
    void getPackages_WithBothFilters_Success() {
        // Given
        List<PackageResponse> packages = Arrays.asList(packageResponse);
        when(packageQueryService.getPackages("Loja ABC", "João Silva")).thenReturn(packages);

        // When
        ResponseEntity<List<PackageResponse>> response = packageQueryController.getPackages(Optional.of("Loja ABC"), Optional.of("João Silva"));

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("pacote-12345", response.getBody().get(0).getId());

        verify(packageQueryService).getPackages("Loja ABC", "João Silva");
    }

    @Test
    void getPackages_WithoutFilters_Success() {
        // Given
        List<PackageResponse> packages = Arrays.asList(packageResponse);
        when(packageQueryService.getPackages(null, null)).thenReturn(packages);

        // When
        ResponseEntity<List<PackageResponse>> response = packageQueryController.getPackages(Optional.empty(), Optional.empty());

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("pacote-12345", response.getBody().get(0).getId());

        verify(packageQueryService).getPackages(null, null);
    }

    @Test
    void getPackages_ServiceThrowsException_ReturnsInternalServerError() {
        // Given
        when(packageQueryService.getPackages(null, null))
            .thenThrow(new RuntimeException("Erro interno do sistema"));

        // When
        ResponseEntity<List<PackageResponse>> response = packageQueryController.getPackages(Optional.empty(), Optional.empty());

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());

        verify(packageQueryService).getPackages(null, null);
    }

    @Test
    void getPackagesAsync_WithFilters_Success() throws ExecutionException, InterruptedException {
        // Given
        List<PackageResponse> packages = Arrays.asList(packageResponse);
        CompletableFuture<List<PackageResponse>> future = CompletableFuture.completedFuture(packages);
        when(packageQueryService.getPackagesAsync("Loja ABC", null)).thenReturn(future);

        // When
        CompletableFuture<ResponseEntity<List<PackageResponse>>> responseFuture = 
            packageQueryController.getPackagesAsync(Optional.of("Loja ABC"), Optional.empty());
        ResponseEntity<List<PackageResponse>> response = responseFuture.get();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        verify(packageQueryService).getPackagesAsync("Loja ABC", null);
    }

    @Test
    void getPackagesPaginated_WithFilters_Success() {
        // Given
        when(packageQueryService.getPackagesPaginated(eq("Loja ABC"), eq(null), any(Pageable.class))).thenReturn(packagePage);

        // When
        ResponseEntity<Page<PackageResponse>> response = packageQueryController.getPackagesPaginated(
            Optional.of("Loja ABC"), Optional.empty(), 0, 20);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        assertEquals(1, response.getBody().getContent().size());

        verify(packageQueryService).getPackagesPaginated(eq("Loja ABC"), eq(null), any(Pageable.class));
    }

    @Test
    void getPackagesPaginatedAsync_WithFilters_Success() throws ExecutionException, InterruptedException {
        // Given
        CompletableFuture<Page<PackageResponse>> future = CompletableFuture.completedFuture(packagePage);
        when(packageQueryService.getPackagesPaginatedAsync(eq("Loja ABC"), eq(null), any(Pageable.class))).thenReturn(future);

        // When
        CompletableFuture<ResponseEntity<Page<PackageResponse>>> responseFuture = 
            packageQueryController.getPackagesPaginatedAsync(Optional.of("Loja ABC"), Optional.empty(), 0, 20);
        ResponseEntity<Page<PackageResponse>> response = responseFuture.get();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());

        verify(packageQueryService).getPackagesPaginatedAsync(eq("Loja ABC"), eq(null), any(Pageable.class));
    }

}
