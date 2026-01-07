package com.hackaton_one.sentiment_api.service;

import com.hackaton_one.sentiment_api.api.dto.HistoryItemDTO;
import com.hackaton_one.sentiment_api.api.dto.HistoryItemListDTO;
import com.hackaton_one.sentiment_api.model.Sentiment;
import com.hackaton_one.sentiment_api.repository.SentimentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HistoryService {
    private final SentimentRepository sentimentRepository;

    public HistoryService(SentimentRepository sentimentRepository) {
        this.sentimentRepository = sentimentRepository;
    }

    public HistoryItemListDTO getHistory() {
        try {
            List<Sentiment> sentiments = sentimentRepository.findTop100ByOrderByAnalyzedAtDesc();

            List<HistoryItemDTO> items = sentiments.stream()
                    .map(s -> new HistoryItemDTO(
                            s.getId(),
                            s.getTextContent(),
                            s.getSentimentResult(),
                            s.getConfidenceScore(),
                            s.getAnalyzedAt()
                    ))
                    .collect(Collectors.toList());

            return new HistoryItemListDTO(items);
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving sentiment history", e);
        }
    }
}
