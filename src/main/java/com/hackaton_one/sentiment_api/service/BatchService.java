package com.hackaton_one.sentiment_api.service;

import com.hackaton_one.sentiment_api.api.dto.BatchSentimentResponseDTO;
import com.hackaton_one.sentiment_api.api.dto.SentimentResponseDTO;
import com.hackaton_one.sentiment_api.api.dto.SentimentResultDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço para processamento em lote de análise de sentimento via CSV.
 */
@Service
public class BatchService {
    @Value("${batch.max-lines:100}")
    private int maxLines;
    
    private final SentimentService sentimentService;

    public BatchService(SentimentService sentimentService) {
        this.sentimentService = sentimentService;
    }

    /**
     * Processa um arquivo CSV contendo textos para análise de sentimento.
     * 
     * @param file Arquivo CSV com os textos
     * @param textColumn Nome da coluna com textos (opcional, usa primeira coluna se null)
     * @return BatchSentimentResponseDTO com resultados
     * @throws Exception em caso de erro de leitura ou parsing
     */
    public BatchSentimentResponseDTO processCSV(MultipartFile file, String textColumn) throws Exception {
        List<SentimentResponseDTO> results = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            String line;
            int textColumnIndex = 0;
            boolean isFirstLine = true;
            int lineCount = 0;
            
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                String[] columns = parseCSVLine(line);
                
                // Primeira linha: header
                if (isFirstLine) {
                    isFirstLine = false;
                    if (textColumn != null && !textColumn.isEmpty()) {
                        textColumnIndex = findColumnIndex(columns, textColumn);
                        if (textColumnIndex == -1) {
                            throw new IllegalArgumentException(
                                "Column '" + textColumn + "' not found. Available: " + String.join(", ", columns));
                        }
                    }
                    continue; // Pula header
                }
                
                // Limite de linhas
                if (lineCount >= maxLines) {
                    break;
                }
                
                if (textColumnIndex >= columns.length) {
                    continue; // Linha incompleta
                }
                
                String text = cleanText(columns[textColumnIndex]);
                if (text.isEmpty()) {
                    continue;
                }
                
                // Analisa sentimento
                SentimentResultDTO result = sentimentService.analyze(text);
                
                results.add(new SentimentResponseDTO(result.previsao(), result.probabilidade(), text));
                lineCount++;
            }
        }
        
        return new BatchSentimentResponseDTO(results, results.size());
    }

    /**
     * Parse de linha CSV respeitando aspas.
     */
    private String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        
        return result.toArray(new String[0]);
    }

    /**
     * Encontra índice da coluna pelo nome (case-insensitive).
     */
    private int findColumnIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            String header = cleanText(headers[i]);
            if (header.equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Remove aspas e espaços em branco do texto.
     */
    private String cleanText(String text) {
        text = text.trim();
        if (text.startsWith("\"") && text.endsWith("\"") && text.length() > 1) {
            text = text.substring(1, text.length() - 1);
        }
        return text.trim();
    }
}
