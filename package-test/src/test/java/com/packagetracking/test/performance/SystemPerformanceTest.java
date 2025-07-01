package com.packagetracking.test.performance;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste de Performance do Sistema de Rastreamento de Pacotes
 * Substitui o script run-performance-tests.sh
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SystemPerformanceTest {

    @LocalServerPort
    private int port;

    private String baseUrl;
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger errorCount = new AtomicInteger(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    private final List<Long> responseTimes = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        RestAssured.port = port;
        setupTestData();
    }

    @Test
    void latencyTest_IndividualResponseTimes() {
        System.out.println("=== TESTE DE LATÊNCIA - TEMPO DE RESPOSTA INDIVIDUAL ===");
        
        int requests = 50;
        List<Long> latencies = new ArrayList<>();

        for (int i = 0; i < requests; i++) {
            long startTime = System.nanoTime();
            
            given()
                .when()
                .get(baseUrl + "/api/packages?page=0&size=10")
                .then()
                .statusCode(200);
            
            long endTime = System.nanoTime();
            long latency = (endTime - startTime) / 1_000_000; // Converter para millisegundos
            
            latencies.add(latency);
        }

        // Calcular estatísticas de latência
        double avgLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0);
        long minLatency = latencies.stream().mapToLong(Long::longValue).min().orElse(0);
        long maxLatency = latencies.stream().mapToLong(Long::longValue).max().orElse(0);
        long p95Latency = calculatePercentile(latencies, 95);
        long p99Latency = calculatePercentile(latencies, 99);

        System.out.println("=== MÉTRICAS DE LATÊNCIA ===");
        System.out.println("Latência Média: " + avgLatency + "ms");
        System.out.println("Latência Mínima: " + minLatency + "ms");
        System.out.println("Latência Máxima: " + maxLatency + "ms");
        System.out.println("P95 Latência: " + p95Latency + "ms");
        System.out.println("P99 Latência: " + p99Latency + "ms");

        // Assertions de latência
        assertTrue(avgLatency < 100, "Latência média deve ser menor que 100ms");
        assertTrue(p95Latency < 200, "P95 deve ser menor que 200ms");
        assertTrue(p99Latency < 500, "P99 deve ser menor que 500ms");
    }

    @Test
    void throughputTest_RequestsPerSecond() throws InterruptedException {
        System.out.println("=== TESTE DE THROUGHPUT - REQUISIÇÕES POR SEGUNDO ===");
        
        int totalRequests = 100;
        int concurrentUsers = 10;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        long startTime = System.currentTimeMillis();

        // Executar requisições concorrentes
        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    long requestStart = System.nanoTime();
                    
                    given()
                        .when()
                        .get(baseUrl + "/api/packages")
                        .then()
                        .statusCode(200);
                    
                    long requestEnd = System.nanoTime();
                    long responseTime = (requestEnd - requestStart) / 1_000_000;
                    
                    responseTimes.add(responseTime);
                    totalResponseTime.addAndGet(responseTime);
                    successCount.incrementAndGet();
                    
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Aguardar conclusão
        latch.await();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double requestsPerSecond = (double) totalRequests / (totalTime / 1000.0);

        System.out.println("=== MÉTRICAS DE THROUGHPUT ===");
        System.out.println("Total de Requisições: " + totalRequests);
        System.out.println("Tempo Total: " + totalTime + "ms");
        System.out.println("Requisições por Segundo: " + String.format("%.2f", requestsPerSecond));
        System.out.println("Taxa de Sucesso: " + (successCount.get() * 100.0 / totalRequests) + "%");
        System.out.println("Taxa de Erro: " + (errorCount.get() * 100.0 / totalRequests) + "%");

        // Assertions de throughput
        assertTrue(requestsPerSecond > 10, "Throughput deve ser maior que 10 req/s");
        assertTrue(successCount.get() > totalRequests * 0.95, "Taxa de sucesso deve ser > 95%");
    }

    @Test
    void resourceUsageTest_MemoryAndCPU() throws InterruptedException {
        System.out.println("=== TESTE DE USO DE RECURSOS - MEMÓRIA E CPU ===");
        
        Runtime runtime = Runtime.getRuntime();
        
        // Medir uso de memória antes
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        // Executar carga de trabalho
        int requests = 50;
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(requests);

        for (int i = 0; i < requests; i++) {
            executor.submit(() -> {
                try {
                    // Simular diferentes tipos de requisição
                    given().when().get(baseUrl + "/api/packages").then().statusCode(200);
                    given().when().get(baseUrl + "/api/packages?page=0&size=20").then().statusCode(200);
                    given().when().get(baseUrl + "/api/packages?sender=Test").then().statusCode(200);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Medir uso de memória após
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;

        System.out.println("=== MÉTRICAS DE RECURSOS ===");
        System.out.println("Memória Antes: " + (memoryBefore / 1024 / 1024) + "MB");
        System.out.println("Memória Depois: " + (memoryAfter / 1024 / 1024) + "MB");
        System.out.println("Memória Utilizada: " + (memoryUsed / 1024 / 1024) + "MB");
        System.out.println("Memória Máxima: " + (runtime.maxMemory() / 1024 / 1024) + "MB");

        // Assertions de recursos
        assertTrue(memoryUsed < 100 * 1024 * 1024, "Uso de memória deve ser < 100MB");
    }

    @Test
    void stressTest_HighLoad_100ConcurrentRequests() throws InterruptedException {
        System.out.println("=== INICIANDO TESTE DE STRESS - 100 REQUISIÇÕES CONCORRENTES ===");
        
        int totalRequests = 100;
        int concurrentUsers = 20;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        long startTime = System.currentTimeMillis();

        // Executar requisições concorrentes
        for (int i = 0; i < totalRequests; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    executeRandomRequest(requestId);
                } finally {
                    latch.countDown();
                }
            });
        }

        // Aguardar conclusão de todas as requisições
        latch.await();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // Calcular e exibir métricas
        printPerformanceMetrics(totalRequests, totalTime);
        
        // Assertions para validar performance
        assertPerformanceCriteria();
    }

    @Test
    void stressTest_ExtremeLoad_500ConcurrentRequests() throws InterruptedException {
        System.out.println("=== INICIANDO TESTE DE STRESS EXTREMO - 500 REQUISIÇÕES ===");
        
        int totalRequests = 500;
        int concurrentUsers = 50;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        long startTime = System.currentTimeMillis();

        // Executar requisições concorrentes
        for (int i = 0; i < totalRequests; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    executeRandomRequest(requestId);
                } finally {
                    latch.countDown();
                }
            });
        }

        // Aguardar conclusão
        latch.await();
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // Calcular e exibir métricas
        printPerformanceMetrics(totalRequests, totalTime);
        
        // Assertions para validar performance em carga extrema
        assertExtremeLoadPerformanceCriteria();
    }

    private void executeRandomRequest(int requestId) {
        try {
            long startTime = System.nanoTime();
            
            // Simular diferentes tipos de requisição
            int requestType = requestId % 4;
            switch (requestType) {
                case 0:
                    // Listar pacotes
                    given().when().get(baseUrl + "/api/packages").then().statusCode(200);
                    break;
                case 1:
                    // Listar com paginação
                    given().when().get(baseUrl + "/api/packages?page=0&size=10").then().statusCode(200);
                    break;
                case 2:
                    // Filtrar por remetente
                    given().when().get(baseUrl + "/api/packages?sender=Test").then().statusCode(200);
                    break;
                case 3:
                    // Filtrar por destinatário
                    given().when().get(baseUrl + "/api/packages?recipient=Test").then().statusCode(200);
                    break;
            }
            
            long endTime = System.nanoTime();
            long responseTime = (endTime - startTime) / 1_000_000;
            
            responseTimes.add(responseTime);
            totalResponseTime.addAndGet(responseTime);
            successCount.incrementAndGet();
            
        } catch (Exception e) {
            errorCount.incrementAndGet();
        }
    }

    private void printPerformanceMetrics(int totalRequests, long totalTime) {
        double requestsPerSecond = (double) totalRequests / (totalTime / 1000.0);
        double avgResponseTime = responseTimes.isEmpty() ? 0 : 
            responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        double successRate = (successCount.get() * 100.0) / totalRequests;
        double errorRate = (errorCount.get() * 100.0) / totalRequests;

        System.out.println("=== MÉTRICAS DE PERFORMANCE ===");
        System.out.println("Total de Requisições: " + totalRequests);
        System.out.println("Tempo Total: " + totalTime + "ms");
        System.out.println("Requisições por Segundo: " + String.format("%.2f", requestsPerSecond));
        System.out.println("Tempo de Resposta Médio: " + String.format("%.2f", avgResponseTime) + "ms");
        System.out.println("Taxa de Sucesso: " + String.format("%.2f", successRate) + "%");
        System.out.println("Taxa de Erro: " + String.format("%.2f", errorRate) + "%");
        System.out.println("Requisições Bem-sucedidas: " + successCount.get());
        System.out.println("Requisições com Erro: " + errorCount.get());
    }

    private void assertPerformanceCriteria() {
        double successRate = (successCount.get() * 100.0) / (successCount.get() + errorCount.get());
        double avgResponseTime = responseTimes.isEmpty() ? 0 : 
            responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);

        assertTrue(successRate >= 95.0, "Taxa de sucesso deve ser >= 95%");
        assertTrue(avgResponseTime < 500, "Tempo de resposta médio deve ser < 500ms");
    }

    private void assertExtremeLoadPerformanceCriteria() {
        double successRate = (successCount.get() * 100.0) / (successCount.get() + errorCount.get());
        double avgResponseTime = responseTimes.isEmpty() ? 0 : 
            responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);

        assertTrue(successRate >= 90.0, "Taxa de sucesso em carga extrema deve ser >= 90%");
        assertTrue(avgResponseTime < 1000, "Tempo de resposta médio em carga extrema deve ser < 1000ms");
    }

    private long calculatePercentile(List<Long> values, int percentile) {
        if (values.isEmpty()) return 0;
        
        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(Long::compareTo);
        
        int index = (int) Math.ceil((percentile / 100.0) * sorted.size()) - 1;
        return sorted.get(Math.max(0, index));
    }

    private void setupTestData() {
        // Criar dados de teste para os testes de performance
        try {
            // Criar alguns pacotes de teste
            for (int i = 0; i < 5; i++) {
                Map<String, Object> packageRequest = Map.of(
                    "description", "Pacote de teste " + i,
                    "sender", "Empresa Teste",
                    "recipient", "Cliente Teste " + i,
                    "estimatedDeliveryDate", "25/07/2025"
                );

                given()
                    .contentType(ContentType.JSON)
                    .body(packageRequest)
                    .when()
                    .post(baseUrl + "/api/packages")
                    .then()
                    .statusCode(201);
            }
        } catch (Exception e) {
            // Ignorar erros na criação de dados de teste
            System.out.println("Aviso: Erro ao criar dados de teste: " + e.getMessage());
        }
    }
} 