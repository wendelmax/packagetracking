package com.packagetracking.query.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        
        cacheManager.setCacheNames(java.util.List.of(
            "packages",
            "packages-list",
            "packages-paginated",
            "packages-by-status",
            "packages-by-sender",
            "packages-by-recipient",
            "tracking-events"
        ));
        
        log.info("Cache Manager configurado para módulo query");
        return cacheManager;
    }
} 