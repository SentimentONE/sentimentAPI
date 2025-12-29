package com.hackaton_one.sentiment_api.exceptions;

import com.hackaton_one.sentiment_api.api.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * Handler global para exceções da API.
 * Centraliza o tratamento de erros e padroniza as respostas HTTP.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Trata exceções relacionadas à inicialização do modelo de Data Science.
     * Retorna HTTP 500 pois representa uma falha interna do servidor.
     */
    @ExceptionHandler(ModelInitializationException.class)
    public ResponseEntity<ApiErrorResponse> handleModelInitializationException(
            ModelInitializationException e) {

        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Model Initialization Error",
                e.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    /**
     * Trata exceções ocorridas durante a análise de sentimento.
     * Retorna HTTP 400 quando o texto não pode ser analisado corretamente.
     */
    @ExceptionHandler(ModelAnalysisException.class)
    public ResponseEntity<ApiErrorResponse> handleModelAnalysisException(
            ModelAnalysisException e) {

        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Analysis Error",
                e.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Trata erros de validação de entrada (@NotNull, @Size, etc).
     * Retorna HTTP 400 com mensagem clara sobre o campo inválido.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Erro de validação");

        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                message,
                LocalDateTime.now()
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Trata exceções genéricas não capturadas.
     * Retorna HTTP 500 para qualquer erro inesperado.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception e) {
        // Log do erro para debug
        e.printStackTrace();
        
        String errorMessage = e.getMessage() != null ? e.getMessage() : "An unexpected error occurred";
        
        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                errorMessage,
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
