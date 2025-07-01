package com.packagetracking.query.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Configuration
@EnableAsync
@Slf4j
public class VirtualThreadConfig {

    private ThreadFactory createVirtualThreadFactory(String prefix) {
        return Thread.ofVirtual()
            .name(prefix + "-", 0)
            .factory();
    }

    @Bean("persistenceExecutor")
    public Executor persistenceExecutor() {
        ExecutorService executor = Executors.newThreadPerTaskExecutor(
            createVirtualThreadFactory("persistence")
        );
        
        log.info("Persistence Virtual Thread Executor configurado para módulo query");
        return executor;
    }

    @Bean("queryExecutor")
    public Executor queryExecutor() {
        ExecutorService executor = Executors.newThreadPerTaskExecutor(
            createVirtualThreadFactory("query")
        );
        
        log.info("Query Virtual Thread Executor configurado");
        return executor;
    }
} 