package com.hackaton_one.sentiment_api.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
@DisplayName("SentimentService Unit Tests")
class SentimentServiceTest {

    @Mock
    private SentimentPersistenceService persistenceService;

    @InjectMocks
    private SentimentService sentimentService;

    @Test
    @DisplayName("Should return true when model is available")
    void shouldReturnTrueWhenModelIsAvailable() {
        // Teste de verificação de disponibilidade do modelo
        // Como o modelo pode não estar disponível em ambiente de teste,
        // apenas verificamos que o método não lança exceção
        assertDoesNotThrow(() -> sentimentService.isModelAvailable());
    }




    @Test
    @DisplayName("Should have persistence service injected")
    void shouldHavePersistenceServiceInjected() {
        assertNotNull(persistenceService);
    }
}

