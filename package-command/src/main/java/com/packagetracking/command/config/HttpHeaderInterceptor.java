package com.packagetracking.command.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;

@Component
@Slf4j
public class HttpHeaderInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        response.setHeader("X-Request-ID", generateRequestId());
        response.setHeader("X-Service-Name", "package-command");
        response.setHeader("X-Timestamp", Instant.now().toString());
        response.setHeader("X-API-Version", "1.0");
        
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        
        log.debug("Requisição processada: {} {} - Request-ID: {}", 
                 request.getMethod(), request.getRequestURI(), response.getHeader("X-Request-ID"));
        
        return true;
    }

    private String generateRequestId() {
        return "req-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
    }
} 