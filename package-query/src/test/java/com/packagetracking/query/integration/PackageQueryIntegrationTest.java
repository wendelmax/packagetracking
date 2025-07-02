package com.packagetracking.query.integration;

import com.packagetracking.query.entity.Package;
import com.packagetracking.query.entity.PackageStatus;
import com.packagetracking.query.entity.TrackingEvent;
import com.packagetracking.query.repository.PackageRepository;
import com.packagetracking.query.repository.TrackingEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PackageQueryIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PackageRepository packageRepository;

    @Autowired
    private TrackingEventRepository trackingEventRepository;

    private MockMvc mockMvc;
    private Package testPackage;
    private TrackingEvent testEvent;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Limpar dados de teste
        trackingEventRepository.deleteAll();
        packageRepository.deleteAll();
        
        // Criar pacote de teste com ID manual
        testPackage = Package.builder()
            .id("pacote-test-123") // ID manual obrigatório
            .description("Pacote de teste")
            .sender("Loja Teste")
            .recipient("Cliente Teste")
            .status(PackageStatus.CREATED)
            .createdAt(Instant.parse("2025-01-20T10:00:00Z"))
            .updatedAt(Instant.parse("2025-01-20T10:00:00Z"))
            .build();
        
        testPackage = packageRepository.save(testPackage);
        
        // Aguardar um pouco para garantir que o pacote foi salvo
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Criar evento de teste
        testEvent = TrackingEvent.builder()
            .id("b2c3d4e5f67890123456789012345678") // UUID válido de 32 caracteres
            .packageId("pacote-test-123")
            .location("Centro de Distribuição Teste")
            .description("Pacote recebido no centro de distribuição")
            .date(LocalDateTime.parse("2025-01-20T11:00:00"))
            .build();
        
        testEvent = trackingEventRepository.save(testEvent);
        
        // Aguardar um pouco para garantir que o evento foi salvo
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void getPackage_WithEvents_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/packages/pacote-test-123")
                .param("includeEvents", "true")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value("pacote-test-123"))
            .andExpect(jsonPath("$.description").value("Pacote de teste"))
            .andExpect(jsonPath("$.sender").value("Loja Teste"))
            .andExpect(jsonPath("$.recipient").value("Cliente Teste"))
            .andExpect(jsonPath("$.status").value("CREATED"))
            .andExpect(jsonPath("$.events").isArray())
            .andExpect(jsonPath("$.events.length()").value(1))
            .andExpect(jsonPath("$.events[0].pacoteId").value("pacote-test-123"))
            .andExpect(jsonPath("$.events[0].localizacao").value("Centro de Distribuição Teste"))
            .andExpect(jsonPath("$.events[0].descricao").value("Pacote recebido no centro de distribuição"));
    }

    @Test
    void getPackage_WithoutEvents_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/packages/pacote-test-123")
                .param("includeEvents", "false")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value("pacote-test-123"))
            .andExpect(jsonPath("$.description").value("Pacote de teste"))
            .andExpect(jsonPath("$.sender").value("Loja Teste"))
            .andExpect(jsonPath("$.recipient").value("Cliente Teste"))
            .andExpect(jsonPath("$.status").value("CREATED"))
            .andExpect(jsonPath("$.events").doesNotExist());
    }

    @Test
    void getPackage_DefaultIncludeEvents_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/packages/pacote-test-123")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value("pacote-test-123"))
            .andExpect(jsonPath("$.events").isArray())
            .andExpect(jsonPath("$.events.length()").value(1));
    }

    @Test
    void getPackage_PackageNotFound_ReturnsNotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/packages/pacote-inexistente")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    void getPackages_WithSenderFilter_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/packages")
                .param("sender", "Loja Teste")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value("pacote-test-123"))
            .andExpect(jsonPath("$[0].sender").value("Loja Teste"));
    }

    @Test
    void getPackages_WithRecipientFilter_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/packages")
                .param("recipient", "Cliente Teste")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value("pacote-test-123"))
            .andExpect(jsonPath("$[0].recipient").value("Cliente Teste"));
    }

    @Test
    void getPackages_WithBothFilters_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/packages")
                .param("sender", "Loja Teste")
                .param("recipient", "Cliente Teste")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value("pacote-test-123"));
    }

    @Test
    void getPackages_WithoutFilters_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/packages")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value("pacote-test-123"));
    }

    @Test
    void getPackagesPaginated_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/packages/page")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value("pacote-test-123"));
    }
}
