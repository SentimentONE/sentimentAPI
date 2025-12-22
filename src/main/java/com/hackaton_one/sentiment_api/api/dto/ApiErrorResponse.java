package com.hackaton_one.sentiment_api.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO padrão para respostas de erro da API.
 * Garante consistência no formato das mensagens retornadas ao cliente.
 */
@Data
@AllArgsConstructor
public class ApiErrorResponse {

    /** Código HTTP do erro */
    private int status;

    /** Tipo ou nome do erro */
    private String error;

    /** Mensagem descritiva para o cliente */
    private String message;

    /** Momento em que o erro ocorreu */
    private LocalDateTime timestamp;
}
