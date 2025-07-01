package com.packagetracking.query.performance;

import com.packagetracking.query.dto.PackageResponse;
import com.packagetracking.query.service.PackageQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PackageQueryPerformanceTest {

    @Autowired
    private PackageQueryService packageQueryService;

    @Test
    void getPackage_WithEvents_PerformanceTest() {
        // Given
        String packageId = "pacote-performance-test";
        int iterations = 100;
        long startTime = System.currentTimeMillis();

        // When
        for (int i = 0; i < iterations; i++) {
            try {
                PackageResponse result = packageQueryService.getPackage(packageId, true);
                assertNotNull(result);
            } catch (RuntimeException e) {
                // Esperado para pacotes que não existem
            }
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double averageTime = (double) totalTime / iterations;

        // Then
        System.out.println("Performance Test - getPackage with events:");
        System.out.println("Total time: " + totalTime + "ms");
        System.out.println("Average time per request: " + averageTime + "ms");
        System.out.println("Requests per second: " + (1000.0 / averageTime));

        // Assert que o tempo médio é razoável (menos de 100ms por requisição)
        assertTrue(averageTime < 100, "Tempo médio muito alto: " + averageTime + "ms");
    }

    @Test
    void getPackage_WithoutEvents_PerformanceTest() {
        // Given
        String packageId = "pacote-performance-test";
        int iterations = 100;
        long startTime = System.currentTimeMillis();

        // When
        for (int i = 0; i < iterations; i++) {
            try {
                PackageResponse result = packageQueryService.getPackage(packageId, false);
                assertNotNull(result);
            } catch (RuntimeException e) {
                // Esperado para pacotes que não existem
            }
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        long averageTime = totalTime / iterations;

        // Then
        System.out.println("Performance Test - getPackage without events:");
        System.out.println("Total time: " + totalTime + "ms");
        System.out.println("Average time per request: " + averageTime + "ms");
        System.out.println("Requests per second: " + (1000.0 / averageTime));

        // Assert que o tempo médio é razoável (menos de 50ms por requisição)
        assertTrue(averageTime < 50, "Tempo médio muito alto: " + averageTime + "ms");
    }

    @Test
    void getPackages_ConcurrentRequests_PerformanceTest() throws InterruptedException, ExecutionException, TimeoutException {
        // Given
        int concurrentRequests = 50;
        long startTime = System.currentTimeMillis();

        // When
        List<CompletableFuture<List<PackageResponse>>> futures = new java.util.ArrayList<>();
        
        for (int i = 0; i < concurrentRequests; i++) {
            CompletableFuture<List<PackageResponse>> future = packageQueryService.getPackagesAsync(null, null);
            futures.add(future);
        }

        // Aguardar todas as requisições
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        long averageTime = totalTime / concurrentRequests;

        // Then
        System.out.println("Performance Test - Concurrent getPackages:");
        System.out.println("Concurrent requests: " + concurrentRequests);
        System.out.println("Total time: " + totalTime + "ms");
        System.out.println("Average time per request: " + averageTime + "ms");
        System.out.println("Requests per second: " + (1000.0 / averageTime));

        // Verificar que todas as requisições foram completadas
        for (CompletableFuture<List<PackageResponse>> future : futures) {
            assertTrue(future.isDone());
            assertNotNull(future.get());
        }

        // Assert que o tempo médio é razoável (menos de 200ms por requisição concorrente)
        assertTrue(averageTime < 200, "Tempo médio muito alto para requisições concorrentes: " + averageTime + "ms");
    }
}
