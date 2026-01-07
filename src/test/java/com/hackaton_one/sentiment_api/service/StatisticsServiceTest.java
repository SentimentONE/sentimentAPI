package com.hackaton_one.sentiment_api.service;

import com.hackaton_one.sentiment_api.api.dto.DailyStatisticsDTO;
import com.hackaton_one.sentiment_api.api.dto.StatisticsDTO;
import com.hackaton_one.sentiment_api.repository.SentimentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatisticsService Unit Tests")
class StatisticsServiceTest {

    @Mock
    private SentimentRepository sentimentRepository;

    @InjectMocks
    private StatisticsService statisticsService;

    @Test
    @DisplayName("Should calculate statistics correctly with balanced data")
    void shouldCalculateStatisticsCorrectlyWithBalancedData() {
        when(sentimentRepository.count()).thenReturn(10L);
        when(sentimentRepository.countBySentimentResult("POSITIVO")).thenReturn(5L);
        when(sentimentRepository.countBySentimentResult("NEGATIVO")).thenReturn(5L);
        when(sentimentRepository.averageConfidence()).thenReturn(0.85);
        when(sentimentRepository.averageConfidenceBySentiment("POSITIVO")).thenReturn(0.90);
        when(sentimentRepository.averageConfidenceBySentiment("NEGATIVO")).thenReturn(0.80);
        when(sentimentRepository.findDailyStatistics(any(LocalDateTime.class))).thenReturn(new ArrayList<>());

        StatisticsDTO result = statisticsService.getStatistics();

        assertNotNull(result);
        assertEquals(10, result.total());
        assertEquals(5, result.positive());
        assertEquals(5, result.negative());
        assertEquals(50.0, result.positivePercentage());
        assertEquals(50.0, result.negativePercentage());
        assertEquals(85.0, result.averageConfidence());
        assertNotNull(result.timeline());
    }

    @Test
    @DisplayName("Should return zero statistics when no data exists")
    void shouldReturnZeroStatisticsWhenNoDataExists() {
        when(sentimentRepository.count()).thenReturn(0L);
        when(sentimentRepository.countBySentimentResult("POSITIVO")).thenReturn(0L);
        when(sentimentRepository.countBySentimentResult("NEGATIVO")).thenReturn(0L);
        when(sentimentRepository.averageConfidence()).thenReturn(0.0);
        when(sentimentRepository.averageConfidenceBySentiment(anyString())).thenReturn(0.0);
        when(sentimentRepository.findDailyStatistics(any(LocalDateTime.class))).thenReturn(new ArrayList<>());

        StatisticsDTO result = statisticsService.getStatistics();

        assertNotNull(result);
        assertEquals(0, result.total());
        assertEquals(0, result.positive());
        assertEquals(0, result.negative());
        assertEquals(0.0, result.positivePercentage());
        assertEquals(0.0, result.negativePercentage());
        assertEquals(0.0, result.averageConfidence());
    }

    @Test
    @DisplayName("Should calculate percentages correctly with unbalanced data")
    void shouldCalculatePercentagesCorrectlyWithUnbalancedData() {
        when(sentimentRepository.count()).thenReturn(100L);
        when(sentimentRepository.countBySentimentResult("POSITIVO")).thenReturn(75L);
        when(sentimentRepository.countBySentimentResult("NEGATIVO")).thenReturn(25L);
        when(sentimentRepository.averageConfidence()).thenReturn(0.90);
        when(sentimentRepository.averageConfidenceBySentiment(anyString())).thenReturn(0.85);
        when(sentimentRepository.findDailyStatistics(any(LocalDateTime.class))).thenReturn(new ArrayList<>());

        StatisticsDTO result = statisticsService.getStatistics();

        assertEquals(100, result.total());
        assertEquals(75, result.positive());
        assertEquals(25, result.negative());
        assertEquals(75.0, result.positivePercentage());
        assertEquals(25.0, result.negativePercentage());
        assertEquals(90.0, result.averageConfidence());
    }

    @Test
    @DisplayName("Should handle only positive sentiments")
    void shouldHandleOnlyPositiveSentiments() {
        when(sentimentRepository.count()).thenReturn(20L);
        when(sentimentRepository.countBySentimentResult("POSITIVO")).thenReturn(20L);
        when(sentimentRepository.countBySentimentResult("NEGATIVO")).thenReturn(0L);
        when(sentimentRepository.averageConfidence()).thenReturn(0.95);
        when(sentimentRepository.averageConfidenceBySentiment(anyString())).thenReturn(0.95);
        when(sentimentRepository.findDailyStatistics(any(LocalDateTime.class))).thenReturn(new ArrayList<>());

        StatisticsDTO result = statisticsService.getStatistics();

        assertEquals(20, result.total());
        assertEquals(20, result.positive());
        assertEquals(0, result.negative());
        assertEquals(100.0, result.positivePercentage());
        assertEquals(0.0, result.negativePercentage());
    }

    @Test
    @DisplayName("Should handle only negative sentiments")
    void shouldHandleOnlyNegativeSentiments() {
        when(sentimentRepository.count()).thenReturn(15L);
        when(sentimentRepository.countBySentimentResult("POSITIVO")).thenReturn(0L);
        when(sentimentRepository.countBySentimentResult("NEGATIVO")).thenReturn(15L);
        when(sentimentRepository.averageConfidence()).thenReturn(0.88);
        when(sentimentRepository.averageConfidenceBySentiment(anyString())).thenReturn(0.88);
        when(sentimentRepository.findDailyStatistics(any(LocalDateTime.class))).thenReturn(new ArrayList<>());

        StatisticsDTO result = statisticsService.getStatistics();

        assertEquals(15, result.total());
        assertEquals(0, result.positive());
        assertEquals(15, result.negative());
        assertEquals(0.0, result.positivePercentage());
        assertEquals(100.0, result.negativePercentage());
    }

    @Test
    @DisplayName("Should throw exception when repository fails")
    void shouldThrowExceptionWhenRepositoryFails() {
        when(sentimentRepository.count()).thenThrow(new RuntimeException("Database error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> statisticsService.getStatistics());

        assertEquals("Database error", exception.getMessage());
    }

    @Test
    @DisplayName("Should include daily statistics in timeline")
    void shouldIncludeDailyStatisticsInTimeline() {
        when(sentimentRepository.count()).thenReturn(10L);
        when(sentimentRepository.countBySentimentResult("POSITIVO")).thenReturn(6L);
        when(sentimentRepository.countBySentimentResult("NEGATIVO")).thenReturn(4L);
        when(sentimentRepository.averageConfidence()).thenReturn(0.85);
        when(sentimentRepository.averageConfidenceBySentiment(anyString())).thenReturn(0.85);

        List<Object[]> dailyData = new ArrayList<>();
        Object[] day1 = {java.sql.Date.valueOf("2026-01-07"), 5L, 3L, 8L};
        Object[] day2 = {java.sql.Date.valueOf("2026-01-06"), 1L, 1L, 2L};
        dailyData.add(day1);
        dailyData.add(day2);

        when(sentimentRepository.findDailyStatistics(any(LocalDateTime.class))).thenReturn(dailyData);

        StatisticsDTO result = statisticsService.getStatistics();

        assertNotNull(result.timeline());
        assertEquals(2, result.timeline().size());

        DailyStatisticsDTO firstDay = result.timeline().getFirst();
        assertEquals(LocalDate.of(2026, 1, 7), firstDay.date());
        assertEquals(5, firstDay.positive());
        assertEquals(3, firstDay.negative());
        assertEquals(8, firstDay.total());
    }

    @Test
    @DisplayName("Should handle null average confidence")
    void shouldHandleNullAverageConfidence() {
        when(sentimentRepository.count()).thenReturn(5L);
        when(sentimentRepository.countBySentimentResult("POSITIVO")).thenReturn(3L);
        when(sentimentRepository.countBySentimentResult("NEGATIVO")).thenReturn(2L);
        when(sentimentRepository.averageConfidence()).thenReturn(null);
        when(sentimentRepository.averageConfidenceBySentiment(anyString())).thenReturn(null);
        when(sentimentRepository.findDailyStatistics(any(LocalDateTime.class))).thenReturn(new ArrayList<>());

        StatisticsDTO result = statisticsService.getStatistics();

        assertNotNull(result);
        assertEquals(0.0, result.averageConfidence());
    }

    @Test
    @DisplayName("Should calculate percentages with decimal precision")
    void shouldCalculatePercentagesWithDecimalPrecision() {
        when(sentimentRepository.count()).thenReturn(3L);
        when(sentimentRepository.countBySentimentResult("POSITIVO")).thenReturn(2L);
        when(sentimentRepository.countBySentimentResult("NEGATIVO")).thenReturn(1L);
        when(sentimentRepository.averageConfidence()).thenReturn(0.85);
        when(sentimentRepository.averageConfidenceBySentiment(anyString())).thenReturn(0.85);
        when(sentimentRepository.findDailyStatistics(any(LocalDateTime.class))).thenReturn(new ArrayList<>());

        StatisticsDTO result = statisticsService.getStatistics();

        assertEquals(66.66666666666667, result.positivePercentage(), 0.00001);
        assertEquals(33.33333333333333, result.negativePercentage(), 0.00001);
    }

    @Test
    @DisplayName("Should fetch statistics for last 7 days")
    void shouldFetchStatisticsForLast7Days() {
        when(sentimentRepository.count()).thenReturn(10L);
        when(sentimentRepository.countBySentimentResult(anyString())).thenReturn(5L);
        when(sentimentRepository.averageConfidence()).thenReturn(0.85);
        when(sentimentRepository.averageConfidenceBySentiment(anyString())).thenReturn(0.85);
        when(sentimentRepository.findDailyStatistics(any(LocalDateTime.class))).thenReturn(new ArrayList<>());

        statisticsService.getStatistics();

        verify(sentimentRepository).findDailyStatistics(any(LocalDateTime.class));
    }
}

