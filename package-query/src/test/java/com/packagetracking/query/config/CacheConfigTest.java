package com.packagetracking.query.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class CacheConfigTest {

    @Mock
    private RedisConnectionFactory redisConnectionFactory;

    private CacheConfig cacheConfig = new CacheConfig();

    @Test
    void cacheManager_ShouldBeCreated() {
        // Act
        CacheManager cacheManager = cacheConfig.cacheManager(redisConnectionFactory);

        // Assert
        assertNotNull(cacheManager);
        assertTrue(cacheManager instanceof org.springframework.data.redis.cache.RedisCacheManager);
    }

    @Test
    void cacheManager_ShouldHaveExpectedCaches() {
        // Act
        CacheManager cacheManager = cacheConfig.cacheManager(redisConnectionFactory);

        // Assert
        assertNotNull(cacheManager.getCache("packages-in-transit"));
    }
}
