package com.packagetracking.command.constants;

/**
 * Constantes relacionadas a APIs externas
 */
public final class ExternalApiConstants {
    
    private ExternalApiConstants() {
    }
    
    public static final String DOG_FACT_TYPE = "fact";
    public static final String DOG_FALLBACK_ID_PREFIX = "fallback-";
    
    public static final String FIXED_HOLIDAY_NAME = "Feriado Fixo";
    public static final String FIXED_HOLIDAY_ENGLISH_NAME = "Fixed Holiday";
    
    public static final String DOG_API_FALLBACK_MESSAGE = "Fallback ativado para Dog API. Retornando fatos padrão.";
    public static final String HOLIDAY_API_FALLBACK_MESSAGE = "Fallback ativado para feriados do ano {}. Usando feriados fixos da configuração.";
    public static final String NO_FIXED_HOLIDAYS_CONFIGURED = "Nenhum feriado fixo configurado para o país {}";
    public static final String HOLIDAY_FALLBACK_ERROR = "Erro no fallback de feriados para o ano {}: {}";
} 