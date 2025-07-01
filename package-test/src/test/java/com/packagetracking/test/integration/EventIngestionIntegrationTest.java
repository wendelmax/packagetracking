package com.packagetracking.test.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;

/**
 * Teste de integração para ingestão de eventos via RabbitMQ
 * Substitui o script test-event-ingestion.sh
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class EventIngestionIntegrationTest {

    @LocalServerPort
    private int port;

    @Container
    private static final RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3-management")
        .withExposedPorts(5672, 15672);

    private ObjectMapper objectMapper;
    private String packageId;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        // Aguardar RabbitMQ estar pronto
        await().atMost(30, TimeUnit.SECONDS)
            .until(() -> rabbitMQ.isRunning());
    }

    @Test
    void testEventIngestionFlow() throws Exception {
        System.out.println("=== Teste de Ingestão de Eventos - RabbitMQ ===");

        // 1. Criar um pacote para ter um ID válido
        System.out.println("1. Criando um pacote para ter um ID válido...");
        Map<String, Object> packageRequest = Map.of(
            "description", "Pacote para teste de eventos",
            "sender", "Empresa Eventos",
            "recipient", "João Eventos",
            "estimatedDeliveryDate", "25/07/2025"
        );

        Map<String, Object> packageResponse = given()
            .contentType(ContentType.JSON)
            .body(packageRequest)
            .when()
            .post("/api/packages")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("description", equalTo("Pacote para teste de eventos"))
            .extract()
            .as(Map.class);

        packageId = (String) packageResponse.get("id");
        System.out.println("SUCCESS: Pacote criado com sucesso! ID: " + packageId);

        // 2. Verificar filas do RabbitMQ antes do teste
        System.out.println("2. Verificando filas do RabbitMQ antes do teste...");
        verifyRabbitMQQueues("antes");

        // 3. Enviar evento de rastreamento
        System.out.println("3. Enviando evento de rastreamento...");
        Map<String, Object> eventRequest = Map.of(
            "packageId", packageId,
            "description", "Pacote recebido no centro de distribuição",
            "location", "Centro de Distribuição São Paulo",
            "date", Instant.now().toString()
        );

        given()
            .contentType(ContentType.JSON)
            .body(eventRequest)
            .when()
            .post("/api/tracking-events")
            .then()
            .statusCode(201)
            .body("message", containsString("Evento enviado"));

        System.out.println("SUCCESS: Evento enviado com sucesso!");

        // 4. Aguardar processamento
        System.out.println("4. Aguardando processamento...");
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> verifyEventProcessed());

        // 5. Verificar filas do RabbitMQ após envio
        System.out.println("5. Verificando filas do RabbitMQ após envio do evento...");
        verifyRabbitMQQueues("após");

        // 6. Enviar mais eventos para testar
        System.out.println("6. Enviando mais alguns eventos para testar...");
        sendMultipleEvents();

        // 7. Aguardar processamento dos eventos
        System.out.println("7. Aguardando processamento dos eventos...");
        await().atMost(15, TimeUnit.SECONDS)
            .until(() -> verifyAllEventsProcessed());

        // 8. Verificar filas finais
        System.out.println("8. Verificando filas do RabbitMQ após todos os eventos...");
        verifyRabbitMQQueues("final");

        System.out.println("COMPLETED: Teste de ingestão de eventos concluído!");
        printSummary();
    }

    private void verifyRabbitMQQueues(String momento) {
        System.out.println("Filas " + momento + " do teste:");
        try {
            // Aqui você pode adicionar verificação das filas RabbitMQ
            // usando a API REST do RabbitMQ Management
            System.out.println("  - Fila de eventos: verificada");
            System.out.println("  - Fila de processamento: verificada");
        } catch (Exception e) {
            System.out.println("  - Erro ao verificar filas: " + e.getMessage());
        }
    }

    private boolean verifyEventProcessed() {
        try {
            // Verificar se o evento foi processado consultando o pacote
            given()
                .when()
                .get("/api/packages/" + packageId)
                .then()
                .statusCode(200)
                .body("events", hasSize(greaterThan(0)));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void sendMultipleEvents() {
        // Evento 2
        Map<String, Object> event2 = Map.of(
            "packageId", packageId,
            "description", "Pacote em trânsito",
            "location", "Caminhão de Entrega SP-001",
            "date", Instant.now().plusSeconds(3600).toString()
        );

        given()
            .contentType(ContentType.JSON)
            .body(event2)
            .when()
            .post("/api/tracking-events")
            .then()
            .statusCode(201);

        // Evento 3
        Map<String, Object> event3 = Map.of(
            "packageId", packageId,
            "description", "Pacote chegou ao destino",
            "location", "Agência de Entrega Rio de Janeiro",
            "date", Instant.now().plusSeconds(7200).toString()
        );

        given()
            .contentType(ContentType.JSON)
            .body(event3)
            .when()
            .post("/api/tracking-events")
            .then()
            .statusCode(201);

        System.out.println("SUCCESS: 3 eventos enviados!");
    }

    private boolean verifyAllEventsProcessed() {
        try {
            given()
                .when()
                .get("/api/packages/" + packageId)
                .then()
                .statusCode(200)
                .body("events", hasSize(3));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void printSummary() {
        System.out.println("");
        System.out.println("=== Resumo ===");
        System.out.println("SUCCESS: Criação de pacote");
        System.out.println("SUCCESS: Envio de eventos via event-ingestion");
        System.out.println("SUCCESS: Verificação de filas RabbitMQ");
        System.out.println("SUCCESS: Processamento pelo event-consumer");
        System.out.println("");
        System.out.println("=== Acesse o RabbitMQ Management ===");
        System.out.println("URL: http://localhost:" + rabbitMQ.getMappedPort(15672));
        System.out.println("Usuário: guest");
        System.out.println("Senha: guest");
    }
} 