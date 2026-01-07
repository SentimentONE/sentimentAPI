package com.hackaton_one.sentiment_api.service;

import com.hackaton_one.sentiment_api.api.dto.HistoryItemDTO;
import com.hackaton_one.sentiment_api.api.dto.HistoryItemListDTO;
import com.hackaton_one.sentiment_api.model.Sentiment;
import com.hackaton_one.sentiment_api.repository.SentimentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HistoryService Unit Tests")
class HistoryServiceTest {

    @Mock
    private SentimentRepository sentimentRepository;

    @InjectMocks
    private HistoryService historyService;

    private List<Sentiment> createSampleSentiments() {
        List<Sentiment> sentiments = new ArrayList<>();

        Sentiment s1 = new Sentiment();
        s1.setId(1L);
        s1.setTextContent("Texto positivo");
        s1.setSentimentResult("POSITIVO");
        s1.setConfidenceScore(0.95);
        s1.setAnalyzedAt(LocalDateTime.now());

        Sentiment s2 = new Sentiment();
        s2.setId(2L);
        s2.setTextContent("Texto negativo");
        s2.setSentimentResult("NEGATIVO");
        s2.setConfidenceScore(0.85);
        s2.setAnalyzedAt(LocalDateTime.now().minusHours(1));

        sentiments.add(s1);
        sentiments.add(s2);

        return sentiments;
    }

    @Test
    @DisplayName("Should return history with valid data")
    void shouldReturnHistoryWithValidData() {
        List<Sentiment> sentiments = createSampleSentiments();
        when(sentimentRepository.findTop100ByOrderByAnalyzedAtDesc()).thenReturn(sentiments);

        HistoryItemListDTO result = historyService.getHistory();

        assertNotNull(result);
        assertNotNull(result.historyItemList());
        assertEquals(2, result.historyItemList().size());

        HistoryItemDTO first = result.historyItemList().getFirst();
        assertEquals(1L, first.id());
        assertEquals("Texto positivo", first.textContent());
        assertEquals("POSITIVO", first.sentimentResult());
        assertEquals(0.95, first.confidenceScore());
        assertNotNull(first.analyzedAt());

        verify(sentimentRepository, times(1)).findTop100ByOrderByAnalyzedAtDesc();
    }

    @Test
    @DisplayName("Should return empty list when no history exists")
    void shouldReturnEmptyListWhenNoHistoryExists() {
        when(sentimentRepository.findTop100ByOrderByAnalyzedAtDesc()).thenReturn(new ArrayList<>());

        HistoryItemListDTO result = historyService.getHistory();

        assertNotNull(result);
        assertNotNull(result.historyItemList());
        assertTrue(result.historyItemList().isEmpty());
        verify(sentimentRepository, times(1)).findTop100ByOrderByAnalyzedAtDesc();
    }

    @Test
    @DisplayName("Should throw exception when repository fails")
    void shouldThrowExceptionWhenRepositoryFails() {
        when(sentimentRepository.findTop100ByOrderByAnalyzedAtDesc())
                .thenThrow(new RuntimeException("Database error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> historyService.getHistory());

        assertTrue(exception.getMessage().contains("Error retrieving sentiment history"));
    }

    @Test
    @DisplayName("Should map all fields correctly")
    void shouldMapAllFieldsCorrectly() {
        Sentiment sentiment = new Sentiment();
        sentiment.setId(99L);
        sentiment.setTextContent("Teste completo");
        sentiment.setSentimentResult("POSITIVO");
        sentiment.setConfidenceScore(0.99);
        sentiment.setAnalyzedAt(LocalDateTime.of(2026, 1, 7, 10, 30));

        when(sentimentRepository.findTop100ByOrderByAnalyzedAtDesc())
                .thenReturn(List.of(sentiment));

        HistoryItemListDTO result = historyService.getHistory();

        HistoryItemDTO item = result.historyItemList().getFirst();
        assertEquals(99L, item.id());
        assertEquals("Teste completo", item.textContent());
        assertEquals("POSITIVO", item.sentimentResult());
        assertEquals(0.99, item.confidenceScore());
        assertEquals(LocalDateTime.of(2026, 1, 7, 10, 30), item.analyzedAt());
    }

    @Test
    @DisplayName("Should handle large result sets")
    void shouldHandleLargeResultSets() {
        List<Sentiment> largeSentiments = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Sentiment s = new Sentiment();
            s.setId((long) i);
            s.setTextContent("Texto " + i);
            s.setSentimentResult(i % 2 == 0 ? "POSITIVO" : "NEGATIVO");
            s.setConfidenceScore(0.8 + (i % 20) * 0.01);
            s.setAnalyzedAt(LocalDateTime.now().minusHours(i));
            largeSentiments.add(s);
        }

        when(sentimentRepository.findTop100ByOrderByAnalyzedAtDesc()).thenReturn(largeSentiments);

        HistoryItemListDTO result = historyService.getHistory();

        assertNotNull(result);
        assertEquals(100, result.historyItemList().size());
    }
}

