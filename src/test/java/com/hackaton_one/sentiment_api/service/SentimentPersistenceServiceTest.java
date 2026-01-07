package com.hackaton_one.sentiment_api.service;

import com.hackaton_one.sentiment_api.model.Sentiment;
import com.hackaton_one.sentiment_api.repository.SentimentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SentimentPersistenceService Unit Tests")
class SentimentPersistenceServiceTest {

    @Mock
    private SentimentRepository sentimentRepository;

    @InjectMocks
    private SentimentPersistenceService persistenceService;

    @BeforeEach
    void setUp() {
        // Mock padrão para save retornar o objeto com ID e triggering @PrePersist
        lenient().when(sentimentRepository.save(any(Sentiment.class))).thenAnswer(invocation -> {
            Sentiment s = invocation.getArgument(0);
            // Simular @PrePersist
            if (s.getAnalyzedAt() == null) {
                s.setAnalyzedAt(java.time.LocalDateTime.now());
            }
            if (s.getId() == null) {
                s.setId(1L);
            }
            return s;
        });
    }

    @Test
    @DisplayName("Should save sentiment with correct data")
    void shouldSaveSentimentWithCorrectData() {
        String textContent = "Texto de teste";
        String sentimentResult = "POSITIVO";
        double confidenceScore = 0.95;

        Sentiment result = persistenceService.saveSentiment(textContent, sentimentResult, confidenceScore);

        assertNotNull(result);
        ArgumentCaptor<Sentiment> captor = ArgumentCaptor.forClass(Sentiment.class);
        verify(sentimentRepository, times(1)).save(captor.capture());

        Sentiment saved = captor.getValue();
        assertEquals(textContent, saved.getTextContent());
        assertEquals(sentimentResult.toUpperCase(), saved.getSentimentResult());
        assertEquals(confidenceScore, saved.getConfidenceScore());
        assertNotNull(saved.getAnalyzedAt());
    }

    @Test
    @DisplayName("Should save positive sentiment")
    void shouldSavePositiveSentiment() {
        String text = "Excelente produto!";
        String sentiment = "POSITIVO";
        double score = 0.98;

        persistenceService.saveSentiment(text, sentiment, score);

        ArgumentCaptor<Sentiment> captor = ArgumentCaptor.forClass(Sentiment.class);
        verify(sentimentRepository).save(captor.capture());

        Sentiment saved = captor.getValue();
        assertEquals("POSITIVO", saved.getSentimentResult());
        assertEquals(0.98, saved.getConfidenceScore());
    }

    @Test
    @DisplayName("Should save negative sentiment")
    void shouldSaveNegativeSentiment() {
        String text = "Péssimo serviço";
        String sentiment = "NEGATIVO";
        double score = 0.92;

        persistenceService.saveSentiment(text, sentiment, score);

        ArgumentCaptor<Sentiment> captor = ArgumentCaptor.forClass(Sentiment.class);
        verify(sentimentRepository).save(captor.capture());

        Sentiment saved = captor.getValue();
        assertEquals("NEGATIVO", saved.getSentimentResult());
        assertEquals(0.92, saved.getConfidenceScore());
    }

    @Test
    @DisplayName("Should set analyzed timestamp automatically")
    void shouldSetAnalyzedTimestampAutomatically() {
        String text = "Texto teste";
        String sentiment = "POSITIVO";
        double score = 0.85;

        persistenceService.saveSentiment(text, sentiment, score);

        ArgumentCaptor<Sentiment> captor = ArgumentCaptor.forClass(Sentiment.class);
        verify(sentimentRepository).save(captor.capture());

        Sentiment saved = captor.getValue();
        assertNotNull(saved.getAnalyzedAt());
    }

    @Test
    @DisplayName("Should handle long text content")
    void shouldHandleLongTextContent() {
        String longText = "a".repeat(1000);
        String sentiment = "POSITIVO";
        double score = 0.80;

        persistenceService.saveSentiment(longText, sentiment, score);

        ArgumentCaptor<Sentiment> captor = ArgumentCaptor.forClass(Sentiment.class);
        verify(sentimentRepository).save(captor.capture());

        Sentiment saved = captor.getValue();
        assertEquals(longText, saved.getTextContent());
    }

    @Test
    @DisplayName("Should handle score at boundaries")
    void shouldHandleScoreAtBoundaries() {
        persistenceService.saveSentiment("text1", "NEGATIVO", 0.0);
        persistenceService.saveSentiment("text2", "POSITIVO", 1.0);
        verify(sentimentRepository, times(2)).save(any(Sentiment.class));
    }

    @Test
    @DisplayName("Should propagate repository exceptions")
    void shouldPropagateRepositoryExceptions() {
        reset(sentimentRepository); // Resetar mock do setUp
        when(sentimentRepository.save(any(Sentiment.class)))
                .thenThrow(new RuntimeException("Database connection failed"));
        assertThrows(RuntimeException.class, () -> persistenceService.saveSentiment("text", "POSITIVO", 0.9));
    }

    @Test
    @DisplayName("Should save multiple sentiments independently")
    void shouldSaveMultipleSentimentsIndependently() {
        String text1 = "Primeiro texto";
        String text2 = "Segundo texto";
        persistenceService.saveSentiment(text1, "POSITIVO", 0.9);
        persistenceService.saveSentiment(text2, "NEGATIVO", 0.8);
        verify(sentimentRepository, times(2)).save(any(Sentiment.class));
    }

    @Test
    @DisplayName("Should handle special characters in text")
    void shouldHandleSpecialCharactersInText() {
        String textWithSpecialChars = "Texto com @#$%&*() caracteres especiais!";
        persistenceService.saveSentiment(textWithSpecialChars, "POSITIVO", 0.85);
        ArgumentCaptor<Sentiment> captor = ArgumentCaptor.forClass(Sentiment.class);
        verify(sentimentRepository).save(captor.capture());
        Sentiment saved = captor.getValue();
        assertEquals(textWithSpecialChars, saved.getTextContent());
    }
}
