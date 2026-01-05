package com.hackaton_one.sentiment_api.api.dto;

import java.util.List;

/**
 * DTO para estatísticas agregadas de sentimentos.
 */
public record StatisticsDTO(
        long total,
        long positive,
        long negative,
        double positivePercentage,
        double negativePercentage,
        double averageConfidence,
        double positiveAverageConfidence,
        double negativeAverageConfidence,
        List<DailyStatisticsDTO> timeline
) {}

