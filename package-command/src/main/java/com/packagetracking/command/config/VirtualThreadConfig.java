package com.packagetracking.command.config;

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

    private ThreadFactory createVirtualThreadFactory() {
        return Thread.ofVirtual()
            .name("external-api" + "-", 0)
            .factory();
    }

    @Bean("externalApiExecutor")
    public Executor externalApiExecutor() {
        ExecutorService executor = Executors.newThreadPerTaskExecutor(
            createVirtualThreadFactory()
        );
        
        log.info("External API Virtual Thread Executor configurado");
        return executor;
    }
} 