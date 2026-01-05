package com.hackaton_one.sentiment_api.service;

import com.hackaton_one.sentiment_api.api.dto.DailyStatisticsDTO;
import com.hackaton_one.sentiment_api.api.dto.StatisticsDTO;
import com.hackaton_one.sentiment_api.repository.SentimentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final SentimentRepository sentimentRepository;

    /**
     * Calcula estatísticas agregadas de todos os sentimentos.
     */
    public StatisticsDTO getStatistics() {
        long total = sentimentRepository.count();
        long positive = sentimentRepository.countBySentimentResult("POSITIVO");
        long negative = sentimentRepository.countBySentimentResult("NEGATIVO");

        double positivePercentage = total > 0 ? (positive * 100.0 / total) : 0.0;
        double negativePercentage = total > 0 ? (negative * 100.0 / total) : 0.0;

        Double avgConfidence = sentimentRepository.averageConfidence();
        Double positiveAvgConfidence = sentimentRepository.averageConfidenceBySentiment("POSITIVO");
        Double negativeAvgConfidence = sentimentRepository.averageConfidenceBySentiment("NEGATIVO");

        double averageConfidence = (avgConfidence != null ? avgConfidence : 0.0) * 100;
        double positiveAverageConfidence = (positiveAvgConfidence != null ? positiveAvgConfidence : 0.0) * 100;
        double negativeAverageConfidence = (negativeAvgConfidence != null ? negativeAvgConfidence : 0.0) * 100;

        // Busca dados dos últimos 7 dias para timeline
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Object[]> dailyStats = sentimentRepository.findDailyStatistics(sevenDaysAgo);
        
        List<DailyStatisticsDTO> timeline = new ArrayList<>();
        for (Object[] row : dailyStats) {
            try {
                LocalDate date;
                // H2 retorna java.sql.Date, PostgreSQL pode retornar LocalDate diretamente
                Object dateObj = row[0];
                if (dateObj instanceof java.sql.Date) {
                    date = ((java.sql.Date) dateObj).toLocalDate();
                } else if (dateObj instanceof LocalDate) {
                    date = (LocalDate) dateObj;
                } else if (dateObj instanceof java.sql.Timestamp) {
                    date = ((java.sql.Timestamp) dateObj).toLocalDateTime().toLocalDate();
                } else {
                    // Tenta converter de String se necessário
                    date = LocalDate.parse(dateObj.toString());
                }
                
                Long positiveCount = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                Long negativeCount = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                Long totalCount = row[3] != null ? ((Number) row[3]).longValue() : 0L;
                
                timeline.add(new DailyStatisticsDTO(date, positiveCount, negativeCount, totalCount));
            } catch (Exception e) {
                log.warn("Erro ao processar estatística diária: {}", e.getMessage());
            }
        }

        return new StatisticsDTO(
                total,
                positive,
                negative,
                positivePercentage,
                negativePercentage,
                averageConfidence,
                positiveAverageConfidence,
                negativeAverageConfidence,
                timeline
        );
    }
}

