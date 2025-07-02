package com.packagetracking.query.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig
@ContextConfiguration(classes = {VirtualThreadConfig.class})
class VirtualThreadConfigTest {

    private final VirtualThreadConfig virtualThreadConfig = new VirtualThreadConfig();

    @Test
    void virtualThreadConfig_ShouldBeCreated() {
        // When & Then
        assertNotNull(virtualThreadConfig);
    }

    @Test
    void virtualThreadConfig_ShouldBeEmpty() {
        // Given
        Class<?> configClass = virtualThreadConfig.getClass();
        
        // When & Then
        // Verifica que não há métodos públicos além dos métodos padrão
        assertTrue(configClass.getDeclaredMethods().length <= 1, 
            "VirtualThreadConfig não deve ter métodos públicos além dos métodos padrão");
    }
}
