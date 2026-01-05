package com.hackaton_one.sentiment_api.api.dto;

import java.time.LocalDateTime;

/**
 * DTO para item do histórico de análises.
 */
public record HistoryItemDTO(
        Long id,
        String textContent,
        String sentimentResult,
        Double confidenceScore,
        LocalDateTime analyzedAt
) {}

