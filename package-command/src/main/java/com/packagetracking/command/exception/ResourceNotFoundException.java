package com.packagetracking.command.exception;

/**
 * Exceção lançada quando um recurso não é encontrado
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static ResourceNotFoundException packageNotFound(String id) {
        return new ResourceNotFoundException("Pacote não encontrado: " + id);
    }
    
    public static ResourceNotFoundException trackingEventNotFound(String id) {
        return new ResourceNotFoundException("Evento de rastreamento não encontrado: " + id);
    }
} 