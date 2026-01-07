package com.hackaton_one.sentiment_api.api.controller;

import com.hackaton_one.sentiment_api.api.dto.*;
import com.hackaton_one.sentiment_api.service.*;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


/**
 * Controller principal da API de análise de sentimento.
 * <p>
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
    private final StatisticsService statisticsService;
    private final HistoryService historyService;

    public SentimentController(
            BatchService batchService,
            SentimentService sentimentService,
            StatisticsService statisticsService,
            HistoryService historyService) {
        this.batchService = batchService;
        this.sentimentService = sentimentService;
        this.statisticsService = statisticsService;
        this.historyService = historyService;
    }

    /**
     * POST /sentiment - Análise de texto único.
     */
    @PostMapping
    public ResponseEntity<SentimentResponseDTO> analyzeSentiment(
            @Valid @RequestBody SentimentRequestDTO request) {

        SentimentResponseDTO response = sentimentService.analyzeAndSave(request.text());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /sentiment/batch - Análise em lote via CSV.
     *
     * @param file       Arquivo CSV (obrigatório)
     * @param textColumn Nome da coluna com textos (opcional)
     */
    @PostMapping(value = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BatchSentimentResponseDTO> analyzeBatchCSV(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "textColumn", required = false) String textColumn) {

        BatchSentimentResponseDTO response = batchService.processCSV(file, textColumn);

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
    public ResponseEntity<HistoryItemListDTO> getHistory() {
        HistoryItemListDTO HistoryService = historyService.getHistory();
        return ResponseEntity.ok(HistoryService);
    }
}
