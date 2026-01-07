package com.hackaton_one.sentiment_api.api.controller;

import com.hackaton_one.sentiment_api.api.dto.HealthCheckDTO;
import com.hackaton_one.sentiment_api.service.SentimentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Controller para verificação de saúde da aplicação.
 *
 * Endpoints:
 * - GET /health - Retorna status da aplicação e do modelo ONNX
 */
@Slf4j
@RestController
@RequestMapping("/health")
public class HealthCheckController {

    private final SentimentService sentimentService;

    public HealthCheckController(SentimentService sentimentService) {
        this.sentimentService = sentimentService;
    }

    /**
     * GET /health - Verifica o status da aplicação.
     *
     * @return HealthCheckDTO com informações de status
     */
    @GetMapping
    public ResponseEntity<HealthCheckDTO> healthCheck() {
        String modelStatus = sentimentService.isModelAvailable() ? "AVAILABLE" : "UNAVAILABLE";

        HealthCheckDTO health = new HealthCheckDTO(
                "UP",
                LocalDateTime.now(),
                modelStatus
        );

        log.debug("Health check requested - Status: UP, Model: {}", modelStatus);

        return ResponseEntity.ok(health);
    }
}

