package com.packagetracking.command.constants;

/**
 * Constantes relacionadas a mensagens e tipos de mensagens
 */
public final class MessageConstants {
    
    private MessageConstants() {
    }
    
    public static final String PACKAGE_MESSAGE_TYPE = "package";
    
    public static final String PACKAGE_NOT_FOUND = "Pacote não encontrado: ";
    public static final String ERROR_UPDATING_PACKAGE_STATUS = "Erro ao atualizar status do pacote";
    public static final String ERROR_CREATING_PACKAGE = "Erro ao criar pacote";
    public static final String ERROR_CANCELING_PACKAGE = "Erro ao cancelar pacote";
    
    public static final String CREATED_STATUS_TRANSITION_ERROR = "Status CREATED só pode transicionar para IN_TRANSIT ou CANCELLED";
    public static final String IN_TRANSIT_STATUS_TRANSITION_ERROR = "Status IN_TRANSIT só pode transicionar para DELIVERED";
    public static final String DELIVERED_STATUS_ERROR = "Pacote já foi entregue, não pode ter status alterado";
    public static final String CANCELLED_STATUS_ERROR = "Pacote cancelado não pode ter status alterado";
    public static final String CANNOT_CANCEL_PACKAGE_IN_TRANSIT = "Não é possível cancelar pacote que já saiu para entrega";
    
    public static final String PACKAGE_CREATED_SUCCESS = "Pacote criado e salvo com sucesso: {}";
    public static final String PACKAGE_STATUS_UPDATED_SUCCESS = "Status do pacote atualizado com sucesso: {} -> {}";
    public static final String PACKAGE_CANCELED_SUCCESS = "Pacote cancelado com sucesso: {}";
    
    public static final String PACKAGE_ID_PREFIX = "pacote-";
} 