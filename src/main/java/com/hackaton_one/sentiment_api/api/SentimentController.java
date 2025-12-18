package com.hackaton_one.sentiment_api.api;

import com.hackaton_one.sentiment_api.model.Sentiment;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller principal da API de análise de sentimento.
 * 
 * Endpoint: POST /sentiment
 * 
 * Responsável por receber requisições de análise de sentimento,
 * validar os dados de entrada e retornar respostas HTTP adequadas.
 */
@RestController
@RequestMapping("/sentiment")
public class SentimentController {

    /**
     * Endpoint POST /sentiment
     * 
     * Recebe uma requisição contendo dados de texto para análise de sentimento.
     * Utiliza a entidade Sentiment com validações Bean Validation.
     * 
     * @param sentiment Entidade Sentiment validada com @Valid
     * @param bindingResult Resultado da validação Bean Validation
     * @return ResponseEntity com HTTP 200 OK e dados do Sentiment em caso de sucesso,
     *         ou HTTP 400 Bad Request com mensagem de erro em caso de falha
     */
    @PostMapping
    public ResponseEntity<?> analyzeSentiment(
            @Valid @RequestBody Sentiment sentiment,
            BindingResult bindingResult) {
        
        try {
            // Verifica se há erros de validação
            if (bindingResult.hasErrors()) {
                String errorMessage = "Dados de entrada inválidos";
                
                FieldError firstError = bindingResult.getFieldErrors().stream()
                    .findFirst()
                    .orElse(null);
                
                if (firstError != null) {
                    errorMessage = firstError.getDefaultMessage() != null ? 
                        firstError.getDefaultMessage() : 
                        "Campo '" + firstError.getField() + "' inválido";
                }
                
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("mensagem", errorMessage);
                
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            // Validação passou - retorna sucesso
            // Stub/mock simples: apenas retorna o objeto recebido
            // (lógica complexa de negócio deve estar em service, não no controller)
            
            return ResponseEntity.ok(sentiment);
            
        } catch (Exception e) {
            // Captura qualquer outra exceção
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("mensagem", "Erro ao processar requisição: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * Captura exceções de validação que ocorrem antes de entrar no método.
     * 
     * @param ex Exceção de validação
     * @return ResponseEntity com HTTP 400 Bad Request e mensagem de erro
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(
            MethodArgumentNotValidException ex) {
        
        String errorMessage = "Dados de entrada inválidos";
        
        if (ex.getBindingResult() != null && ex.getBindingResult().hasFieldErrors()) {
            FieldError firstError = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .orElse(null);
            
            if (firstError != null) {
                errorMessage = firstError.getDefaultMessage() != null ? 
                    firstError.getDefaultMessage() : 
                    "Campo '" + firstError.getField() + "' inválido";
            }
        }
        
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("mensagem", errorMessage);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Captura exceções quando o corpo da requisição está ausente ou malformado.
     * 
     * @param ex Exceção de leitura do corpo da requisição
     * @return ResponseEntity com HTTP 400 Bad Request e mensagem de erro
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleMessageNotReadableException(
            HttpMessageNotReadableException ex) {
        
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("mensagem", "Corpo da requisição ausente ou inválido");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}

