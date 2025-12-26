package com.hackaton_one.sentiment_api.api.dto;

/**
 * Resultado da análise de sentimento do modelo ONNX.
 *
 * @param previsao Label retornada pelo modelo (ex: "Positivo", "Negativo")
 * @param probabilidade Probabilidade/confiança da previsão (0.0 a 1.0)
 */
public record SentimentResultDTO(
        String previsao,
        double probabilidade
) {}
