package com.packagetracking.query.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig
@ContextConfiguration(classes = {VirtualThreadConfig.class})
class VirtualThreadConfigTest {

    private final VirtualThreadConfig virtualThreadConfig = new VirtualThreadConfig();

    @Test
    void persistenceExecutor_ShouldBeCreated() {
        // When
        Executor executor = virtualThreadConfig.persistenceExecutor();

        // Then
        assertNotNull(executor);
        assertTrue(executor instanceof java.util.concurrent.ExecutorService);
    }

    @Test
    void queryExecutor_ShouldBeCreated() {
        // When
        Executor executor = virtualThreadConfig.queryExecutor();

        // Then
        assertNotNull(executor);
        assertTrue(executor instanceof java.util.concurrent.ExecutorService);
    }

    @Test
    void persistenceExecutor_ShouldBeDifferentFromQueryExecutor() {
        // When
        Executor persistenceExecutor = virtualThreadConfig.persistenceExecutor();
        Executor queryExecutor = virtualThreadConfig.queryExecutor();

        // Then
        assertNotNull(persistenceExecutor);
        assertNotNull(queryExecutor);
        assertNotSame(persistenceExecutor, queryExecutor);
    }
}
