package com.hackaton_one.sentiment_api.api.dto;

import java.util.List;

/**
 * Resposta da análise de sentimento em lote via CSV.
 *
 * @param results Lista de resultados individuais
 * @param totalProcessed Quantidade de textos processados
 */
public record BatchSentimentResponseDTO(
        List<SentimentResponseDTO> results,
        int totalProcessed
) {}
