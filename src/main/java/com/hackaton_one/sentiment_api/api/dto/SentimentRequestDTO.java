package com.hackaton_one.sentiment_api.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de entrada para análise de sentimento.
 *
 * Exemplo JSON:
 * {
 *   "text": "Este produto é muito bom!"
 * }
 */
public record SentimentRequestDTO(

        @NotBlank(message = "O campo 'text' é obrigatório")
        String text

) {}