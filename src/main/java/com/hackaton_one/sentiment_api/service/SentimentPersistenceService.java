package com.hackaton_one.sentiment_api.service;

import com.hackaton_one.sentiment_api.model.Sentiment;
import com.hackaton_one.sentiment_api.repository.SentimentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SentimentPersistenceService {

    private final SentimentRepository sentimentRepository;

    /**
     * Salva uma análise de sentimento no banco de dados.
     */
    @Transactional
    public Sentiment saveSentiment(String text, String sentiment, double score) {
        try {
            Sentiment sentimentEntity = new Sentiment();
            sentimentEntity.setTextContent(text);
            sentimentEntity.setSentimentResult(sentiment.toUpperCase());
            sentimentEntity.setConfidenceScore(score);
            
            Sentiment saved = sentimentRepository.save(sentimentEntity);
            log.debug("Análise salva com sucesso: ID={}, Sentiment={}", saved.getId(), saved.getSentimentResult());
            return saved;
        } catch (Exception e) {
            log.error("Erro ao salvar análise no banco: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao salvar análise", e);
        }
    }
}

