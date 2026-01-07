package com.hackaton_one.sentiment_api.api.dto;

import java.time.LocalDateTime;

/**
 * DTO para resposta do healthcheck.
 */
public record HealthCheckDTO(
        String status,
        LocalDateTime timestamp,
        String modelStatus
) {}

