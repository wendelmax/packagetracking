package com.packagetracking.command.util;

import java.util.UUID;

/**
 * Utilitário para geração de UUIDs otimizados
 */
public class UuidGenerator {
    
    /**
     * Gera um UUID v4 (aleatório) otimizado para performance
     * Remove hífens para reduzir tamanho e melhorar performance de índices
     */
    public static String generateOptimizedUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * Gera um UUID v4 padrão (com hífens)
     */
    public static String generateStandardUuid() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Gera um UUID v4 com prefixo personalizado
     */
    public static String generateUuidWithPrefix(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString();
    }
} 