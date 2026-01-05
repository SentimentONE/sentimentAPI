package com.hackaton_one.sentiment_api.api.controller;

import com.hackaton_one.sentiment_api.api.dto.BatchSentimentResponseDTO;
import com.hackaton_one.sentiment_api.api.dto.HistoryItemDTO;
import com.hackaton_one.sentiment_api.api.dto.SentimentRequestDTO;
import com.hackaton_one.sentiment_api.api.dto.SentimentResponseDTO;
import com.hackaton_one.sentiment_api.api.dto.StatisticsDTO;
import com.hackaton_one.sentiment_api.model.Sentiment;
import com.hackaton_one.sentiment_api.repository.SentimentRepository;
import com.hackaton_one.sentiment_api.service.BatchService;
import com.hackaton_one.sentiment_api.service.SentimentPersistenceService;
import com.hackaton_one.sentiment_api.service.SentimentService;
import com.hackaton_one.sentiment_api.service.StatisticsService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;


/**
 * Controller principal da API de análise de sentimento.
 * 
 * Endpoints:
 * - POST /sentiment (texto único)
 * - POST /sentiment/batch (CSV em lote)
 * - GET /sentiment/statistics (estatísticas agregadas)
 * - GET /sentiment/history (histórico de análises)
 */
@Slf4j
@RestController
@RequestMapping("/sentiment")
public class SentimentController {

    private final BatchService batchService;
    private final SentimentService sentimentService;
    private final SentimentPersistenceService persistenceService;
    private final StatisticsService statisticsService;
    private final SentimentRepository sentimentRepository;

    public SentimentController(
            BatchService batchService, 
            SentimentService sentimentService,
            SentimentPersistenceService persistenceService,
            StatisticsService statisticsService,
            SentimentRepository sentimentRepository) {
        this.batchService = batchService;
        this.sentimentService = sentimentService;
        this.persistenceService = persistenceService;
        this.statisticsService = statisticsService;
        this.sentimentRepository = sentimentRepository;
    }

    /**
     * POST /sentiment - Análise de texto único.
     */
    @PostMapping
    public ResponseEntity<SentimentResponseDTO> analyzeSentiment(
            @Valid @RequestBody SentimentRequestDTO request) {

        var result = sentimentService.analyze(request.text());
        
        String sentiment = result.previsao().toUpperCase();
        double score = result.probabilidade();
        
        // Salva a análise no banco de dados
        try {
            persistenceService.saveSentiment(request.text(), sentiment, score);
        } catch (Exception e) {
            log.warn("Erro ao salvar análise no banco (continuando): {}", e.getMessage());
        }

        return ResponseEntity.ok(
                new SentimentResponseDTO(
                    sentiment,
                    score,
                    request.text()
                )
        );
    }

    /**
     * POST /sentiment/batch - Análise em lote via CSV.
     * 
     * @param file Arquivo CSV (obrigatório)
     * @param textColumn Nome da coluna com textos (opcional)
     */
    @PostMapping(value = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BatchSentimentResponseDTO> analyzeBatchCSV(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "textColumn", required = false) String textColumn) {
        
        if (file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is required");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("File must have .csv extension");
        }

        BatchSentimentResponseDTO response = batchService.processCSV(file, textColumn);

        if (response.totalProcessed() == 0) {
            throw new IllegalArgumentException("No valid text found in CSV");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * GET /sentiment/statistics - Retorna estatísticas agregadas.
     */
    @GetMapping("/statistics")
    public ResponseEntity<StatisticsDTO> getStatistics() {
        StatisticsDTO statistics = statisticsService.getStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * GET /sentiment/history - Retorna histórico de análises (últimas 100).
     */
    @GetMapping("/history")
    public ResponseEntity<List<HistoryItemDTO>> getHistory() {
        List<Sentiment> sentiments = sentimentRepository.findTop100ByOrderByAnalyzedAtDesc();
        
        List<HistoryItemDTO> history = sentiments.stream()
                .map(s -> new HistoryItemDTO(
                        s.getId(),
                        s.getTextContent(),
                        s.getSentimentResult(),
                        s.getConfidenceScore(),
                        s.getAnalyzedAt()
                ))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(history);
    }
}
