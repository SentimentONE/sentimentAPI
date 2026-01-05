package com.hackaton_one.sentiment_api.api.dto;

import java.time.LocalDate;

/**
 * DTO para estatísticas diárias.
 */
public record DailyStatisticsDTO(
        LocalDate date,
        long positive,
        long negative,
        long total
) {}

