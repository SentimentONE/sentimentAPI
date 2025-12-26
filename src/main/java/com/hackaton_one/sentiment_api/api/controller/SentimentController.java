package com.hackaton_one.sentiment_api.api.controller;

import com.hackaton_one.sentiment_api.api.dto.BatchSentimentResponseDTO;
import com.hackaton_one.sentiment_api.api.dto.SentimentResponseDTO;
import com.hackaton_one.sentiment_api.service.BatchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller principal da API de análise de sentimento.
 * 
 * Endpoints:
 * - POST /sentiment (texto único)
 * - POST /sentiment/batch (CSV em lote)
 */
@RestController
@RequestMapping("/sentiment")
public class SentimentController {

    private final BatchService batchService;

    public SentimentController(BatchService batchService) {
        this.batchService = batchService;
    }

    /**
     * POST /sentiment - Análise de texto único.
     */
    @PostMapping
    public ResponseEntity<?> analyzeSentiment(
            @Valid @RequestBody SentimentResponseDTO request,
            BindingResult bindingResult) {
        
        try {
            if (bindingResult.hasErrors()) {
                String errorMessage = "Invalid input data";
                
                FieldError firstError = bindingResult.getFieldErrors().stream()
                    .findFirst()
                    .orElse(null);
                
                if (firstError != null) {
                    errorMessage = firstError.getDefaultMessage() != null ? 
                        firstError.getDefaultMessage() : 
                        "Field '" + firstError.getField() + "' is invalid";
                }
                
                return errorResponse(errorMessage, HttpStatus.BAD_REQUEST);
            }
            
            return ResponseEntity.ok(
                    new SentimentResponseDTO("POSITIVE", 0.87, request.text())
            );
            
        } catch (Exception e) {
            return errorResponse("Error processing request: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * POST /sentiment/batch - Análise em lote via CSV.
     * 
     * @param file Arquivo CSV (obrigatório)
     * @param textColumn Nome da coluna com textos (opcional)
     */
    @PostMapping(value = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> analyzeBatchCSV(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "textColumn", required = false) String textColumn) {
        
        try {
            if (file.isEmpty()) {
                return errorResponse("CSV file is required", HttpStatus.BAD_REQUEST);
            }
            
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
                return errorResponse("File must have .csv extension", HttpStatus.BAD_REQUEST);
            }
            
            BatchSentimentResponseDTO response = batchService.processCSV(file, textColumn);
            
            if (response.totalProcessed() == 0) {
                return errorResponse("No valid text found in CSV", HttpStatus.BAD_REQUEST);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return errorResponse("Error processing CSV: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(
            MethodArgumentNotValidException ex) {
        
        String errorMessage = "Invalid input data";
        
        if (ex.getBindingResult() != null && ex.getBindingResult().hasFieldErrors()) {
            FieldError firstError = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .orElse(null);
            
            if (firstError != null) {
                errorMessage = firstError.getDefaultMessage() != null ? 
                    firstError.getDefaultMessage() : 
                    "Field '" + firstError.getField() + "' is invalid";
            }
        }
        
        Map<String, String> errorRes = new HashMap<>();
        errorRes.put("message", errorMessage);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorRes);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleMessageNotReadableException(
            HttpMessageNotReadableException ex) {
        
        Map<String, String> errorRes = new HashMap<>();
        errorRes.put("message", "Request body is missing or invalid");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorRes);
    }

    private ResponseEntity<Map<String, String>> errorResponse(String message, HttpStatus status) {
        Map<String, String> error = new HashMap<>();
        error.put("message", message);
        return ResponseEntity.status(status).body(error);
    }
}
