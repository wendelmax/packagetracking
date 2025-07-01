package com.packagetracking.test.e2e;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;

/**
 * Teste End-to-End do Sistema de Rastreamento de Pacotes
 * Substitui o script e2e-test-working.sh
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class PackageTrackingE2ETest {

    @LocalServerPort
    private int port;

    @Container
    private static final MySQLContainer<?> mysqlMaster = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("packagetracking")
        .withUsername("app_write")
        .withPassword("app_write")
        .withInitScript("init-master.sql");

    @Container
    private static final MySQLContainer<?> mysqlSlave = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("packagetracking")
        .withUsername("app_read")
        .withPassword("app_read")
        .withInitScript("init-slave.sql");

    @Container
    private static final RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3-management")
        .withExposedPorts(5672, 15672);

    private String packageId;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        
        // Aguardar containers estarem prontos
        await().atMost(60, TimeUnit.SECONDS)
            .until(() -> mysqlMaster.isRunning() && mysqlSlave.isRunning() && rabbitMQ.isRunning());
    }

    @Test
    void testCompletePackageTrackingFlow() {
        System.out.println("=== Teste Ponta a Ponta - Sistema de Rastreamento de Pacotes ===");

        // 1. Criar um novo pacote
        System.out.println("1. Criando um novo pacote...");
        Map<String, Object> packageRequest = Map.of(
            "description", "Pacote de teste E2E",
            "sender", "Empresa Teste",
            "recipient", "Maria Silva",
            "estimatedDeliveryDate", "20/07/2025"
        );

        Map<String, Object> packageResponse = given()
            .contentType(ContentType.JSON)
            .body(packageRequest)
            .when()
            .post("/api/packages")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("description", equalTo("Pacote de teste E2E"))
            .body("sender", equalTo("Empresa Teste"))
            .body("recipient", equalTo("Maria Silva"))
            .extract()
            .as(Map.class);

        packageId = (String) packageResponse.get("id");
        System.out.println("SUCCESS: Pacote criado com sucesso! ID: " + packageId);

        // 2. Aguardar replicação para o banco de leitura
        System.out.println("2. Aguardando replicação para o banco de leitura...");
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> verifyPackageInReadDatabase());

        // 3. Consultar o pacote no módulo de leitura
        System.out.println("3. Consultando o pacote no módulo de leitura...");
        Map<String, Object> queryResponse = given()
            .when()
            .get("/api/packages/" + packageId)
            .then()
            .statusCode(200)
            .body("id", equalTo(packageId))
            .body("description", equalTo("Pacote de teste E2E"))
            .body("sender", equalTo("Empresa Teste"))
            .body("recipient", equalTo("Maria Silva"))
            .extract()
            .as(Map.class);

        System.out.println("SUCCESS: Pacote consultado com sucesso!");

        // 4. Listar todos os pacotes
        System.out.println("4. Listando todos os pacotes...");
        given()
            .when()
            .get("/api/packages")
            .then()
            .statusCode(200)
            .body("$", hasSize(greaterThan(0)))
            .body("[0].id", notNullValue());

        System.out.println("SUCCESS: Lista de pacotes obtida com sucesso!");

        // 5. Atualizar status do pacote
        System.out.println("5. Atualizando status do pacote...");
        Map<String, Object> statusUpdate = Map.of("status", "IN_TRANSIT");

        given()
            .contentType(ContentType.JSON)
            .body(statusUpdate)
            .when()
            .put("/api/packages/" + packageId + "/status")
            .then()
            .statusCode(200)
            .body("status", equalTo("IN_TRANSIT"));

        System.out.println("SUCCESS: Status atualizado com sucesso!");

        // 6. Verificar status atualizado
        System.out.println("6. Verificando status atualizado...");
        await().atMost(5, TimeUnit.SECONDS)
            .until(() -> verifyStatusUpdated());

        Map<String, Object> finalResponse = given()
            .when()
            .get("/api/packages/" + packageId)
            .then()
            .statusCode(200)
            .body("status", equalTo("IN_TRANSIT"))
            .extract()
            .as(Map.class);

        System.out.println("SUCCESS: Status verificado com sucesso!");

        // 7. Testar eventos de rastreamento
        System.out.println("7. Testando eventos de rastreamento...");
        testTrackingEvents();

        System.out.println("COMPLETED: Teste ponta a ponta concluído com sucesso!");
        printSummary();
    }

    private boolean verifyPackageInReadDatabase() {
        try {
            given()
                .when()
                .get("/api/packages/" + packageId)
                .then()
                .statusCode(200);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean verifyStatusUpdated() {
        try {
            given()
                .when()
                .get("/api/packages/" + packageId)
                .then()
                .statusCode(200)
                .body("status", equalTo("IN_TRANSIT"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void testTrackingEvents() {
        // Enviar evento de rastreamento
        Map<String, Object> eventRequest = Map.of(
            "packageId", packageId,
            "description", "Pacote em trânsito para entrega",
            "location", "Centro de Distribuição",
            "date", java.time.Instant.now().toString()
        );

        given()
            .contentType(ContentType.JSON)
            .body(eventRequest)
            .when()
            .post("/api/tracking-events")
            .then()
            .statusCode(201);

        // Aguardar processamento do evento
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> verifyEventProcessed());

        System.out.println("SUCCESS: Eventos de rastreamento testados com sucesso!");
    }

    private boolean verifyEventProcessed() {
        try {
            given()
                .when()
                .get("/api/packages/" + packageId + "?includeEvents=true")
                .then()
                .statusCode(200)
                .body("events", hasSize(greaterThan(0)));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void printSummary() {
        System.out.println("");
        System.out.println("=== Resumo ===");
        System.out.println("SUCCESS: Criação de pacote (módulo command)");
        System.out.println("SUCCESS: Replicação para banco de leitura");
        System.out.println("SUCCESS: Consulta de pacote (módulo query)");
        System.out.println("SUCCESS: Listagem de pacotes");
        System.out.println("SUCCESS: Atualização de status");
        System.out.println("SUCCESS: Verificação de status atualizado");
        System.out.println("SUCCESS: Eventos de rastreamento");
        System.out.println("");
        System.out.println("=== Status dos Serviços ===");
        System.out.println("PACKAGE: package-ingestion: Serviço de ingestão de pacotes (porta " + port + ")");
        System.out.println("EVENT: event-ingestion: Serviço de ingestão de eventos");
        System.out.println("CONSUMER: event-consumer: Consumidor de eventos");
        System.out.println("QUERY: package-query: Serviço de consulta");
        System.out.println("DATABASE: MySQL Master: " + mysqlMaster.getJdbcUrl());
        System.out.println("DATABASE: MySQL Slave: " + mysqlSlave.getJdbcUrl());
        System.out.println("MESSAGE_QUEUE: RabbitMQ: " + rabbitMQ.getAmqpUrl());
    }
} 