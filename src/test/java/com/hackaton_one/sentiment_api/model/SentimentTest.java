package com.hackaton_one.sentiment_api.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Sentiment Entity Unit Tests")
class SentimentTest {

    @Test
    @DisplayName("Should create sentiment with default constructor")
    void shouldCreateSentimentWithDefaultConstructor() {
        // When
        Sentiment sentiment = new Sentiment();

        // Then
        assertNotNull(sentiment);
        assertNull(sentiment.getId());
        assertNull(sentiment.getTextContent());
        assertNull(sentiment.getSentimentResult());
        assertNull(sentiment.getConfidenceScore());
        assertNull(sentiment.getAnalyzedAt());
    }

    @Test
    @DisplayName("Should set and get id correctly")
    void shouldSetAndGetIdCorrectly() {
        // Given
        Sentiment sentiment = new Sentiment();
        Long expectedId = 123L;

        // When
        sentiment.setId(expectedId);

        // Then
        assertEquals(expectedId, sentiment.getId());
    }

    @Test
    @DisplayName("Should set and get text content correctly")
    void shouldSetAndGetTextContentCorrectly() {
        // Given
        Sentiment sentiment = new Sentiment();
        String expectedText = "Este é um texto de teste";

        // When
        sentiment.setTextContent(expectedText);

        // Then
        assertEquals(expectedText, sentiment.getTextContent());
    }

    @Test
    @DisplayName("Should set and get sentiment result correctly")
    void shouldSetAndGetSentimentResultCorrectly() {
        // Given
        Sentiment sentiment = new Sentiment();
        String expectedSentiment = "POSITIVO";

        // When
        sentiment.setSentimentResult(expectedSentiment);

        // Then
        assertEquals(expectedSentiment, sentiment.getSentimentResult());
    }

    @Test
    @DisplayName("Should set and get confidence score correctly")
    void shouldSetAndGetConfidenceScoreCorrectly() {
        // Given
        Sentiment sentiment = new Sentiment();
        Double expectedScore = 0.95;

        // When
        sentiment.setConfidenceScore(expectedScore);

        // Then
        assertEquals(expectedScore, sentiment.getConfidenceScore());
    }

    @Test
    @DisplayName("Should set and get analyzed at correctly")
    void shouldSetAndGetAnalyzedAtCorrectly() {
        // Given
        Sentiment sentiment = new Sentiment();
        LocalDateTime expectedTime = LocalDateTime.of(2026, 1, 7, 10, 30);

        // When
        sentiment.setAnalyzedAt(expectedTime);

        // Then
        assertEquals(expectedTime, sentiment.getAnalyzedAt());
    }

    @Test
    @DisplayName("Should create complete sentiment object")
    void shouldCreateCompleteSentimentObject() {
        // Given
        Sentiment sentiment = new Sentiment();

        // When
        sentiment.setId(1L);
        sentiment.setTextContent("Texto positivo de teste");
        sentiment.setSentimentResult("POSITIVO");
        sentiment.setConfidenceScore(0.98);
        sentiment.setAnalyzedAt(LocalDateTime.now());

        // Then
        assertEquals(1L, sentiment.getId());
        assertEquals("Texto positivo de teste", sentiment.getTextContent());
        assertEquals("POSITIVO", sentiment.getSentimentResult());
        assertEquals(0.98, sentiment.getConfidenceScore());
        assertNotNull(sentiment.getAnalyzedAt());
    }

    @Test
    @DisplayName("Should handle negative sentiment")
    void shouldHandleNegativeSentiment() {
        // Given
        Sentiment sentiment = new Sentiment();

        // When
        sentiment.setSentimentResult("NEGATIVO");
        sentiment.setConfidenceScore(0.85);

        // Then
        assertEquals("NEGATIVO", sentiment.getSentimentResult());
        assertEquals(0.85, sentiment.getConfidenceScore());
    }

    @Test
    @DisplayName("Should handle null values")
    void shouldHandleNullValues() {
        // Given
        Sentiment sentiment = new Sentiment();

        // When
        sentiment.setTextContent(null);
        sentiment.setSentimentResult(null);
        sentiment.setConfidenceScore(null);
        sentiment.setAnalyzedAt(null);

        // Then
        assertNull(sentiment.getTextContent());
        assertNull(sentiment.getSentimentResult());
        assertNull(sentiment.getConfidenceScore());
        assertNull(sentiment.getAnalyzedAt());
    }

    @Test
    @DisplayName("Should handle long text content")
    void shouldHandleLongTextContent() {
        // Given
        Sentiment sentiment = new Sentiment();
        String longText = "a".repeat(5000);

        // When
        sentiment.setTextContent(longText);

        // Then
        assertEquals(longText, sentiment.getTextContent());
        assertEquals(5000, sentiment.getTextContent().length());
    }

    @Test
    @DisplayName("Should handle special characters in text")
    void shouldHandleSpecialCharactersInText() {
        // Given
        Sentiment sentiment = new Sentiment();
        String specialText = "Texto com @#$%&*() caracteres! ñ ç á é";

        // When
        sentiment.setTextContent(specialText);

        // Then
        assertEquals(specialText, sentiment.getTextContent());
    }

    @Test
    @DisplayName("Should handle boundary confidence scores")
    void shouldHandleBoundaryConfidenceScores() {
        // Given
        Sentiment sentiment1 = new Sentiment();
        Sentiment sentiment2 = new Sentiment();

        // When
        sentiment1.setConfidenceScore(0.0);
        sentiment2.setConfidenceScore(1.0);

        // Then
        assertEquals(0.0, sentiment1.getConfidenceScore());
        assertEquals(1.0, sentiment2.getConfidenceScore());
    }

    @Test
    @DisplayName("Should update values after initial set")
    void shouldUpdateValuesAfterInitialSet() {
        // Given
        Sentiment sentiment = new Sentiment();
        sentiment.setTextContent("Texto inicial");
        sentiment.setSentimentResult("POSITIVO");
        sentiment.setConfidenceScore(0.8);

        // When
        sentiment.setTextContent("Texto atualizado");
        sentiment.setSentimentResult("NEGATIVO");
        sentiment.setConfidenceScore(0.9);

        // Then
        assertEquals("Texto atualizado", sentiment.getTextContent());
        assertEquals("NEGATIVO", sentiment.getSentimentResult());
        assertEquals(0.9, sentiment.getConfidenceScore());
    }
}

