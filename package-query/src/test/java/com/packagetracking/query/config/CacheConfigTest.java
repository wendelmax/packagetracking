package com.packagetracking.query.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig
@ContextConfiguration(classes = {CacheConfig.class})
class CacheConfigTest {

    private final CacheConfig cacheConfig = new CacheConfig();

    @Test
    void cacheManager_ShouldBeCreated() {
        // When
        CacheManager cacheManager = cacheConfig.cacheManager();

        // Then
        assertNotNull(cacheManager);
        assertTrue(cacheManager instanceof org.springframework.cache.concurrent.ConcurrentMapCacheManager);
    }

    @Test
    void cacheManager_ShouldHaveExpectedCaches() {
        // When
        CacheManager cacheManager = cacheConfig.cacheManager();

        // Then
        assertNotNull(cacheManager);
        
        // Verificar se os caches esperados existem
        assertNotNull(cacheManager.getCache("packages"));
        assertNotNull(cacheManager.getCache("packages-list"));
        assertNotNull(cacheManager.getCache("packages-paginated"));
        assertNotNull(cacheManager.getCache("packages-by-status"));
        assertNotNull(cacheManager.getCache("packages-by-sender"));
        assertNotNull(cacheManager.getCache("packages-by-recipient"));
        assertNotNull(cacheManager.getCache("tracking-events"));
    }
}
