package com.hackaton_one.sentiment_api.integration;

import com.hackaton_one.sentiment_api.api.controller.SentimentController;
import com.hackaton_one.sentiment_api.api.dto.*;
import com.hackaton_one.sentiment_api.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SentimentController.class)
public class SentimentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SentimentService sentimentService;

    @MockitoBean
    private BatchService batchService;

    @MockitoBean
    private StatisticsService statisticsService;

    @MockitoBean
    private HistoryService historyService;

    @MockitoBean
    private SentimentPersistenceService sentimentPersistenceService;

    /* Test analyze sentiment single text endpoint */
    @Nested
    @DisplayName("Tests for /sentiment endpoint")
    class AnalyzeSentimentTests {
        @Test
        void shouldReturn200WhenSendingPostToAnalyzeWithPositiveSentiment() throws Exception {
            when(sentimentService.analyzeAndSave(anyString()))
                    .thenReturn(new SentimentResponseDTO("POSITIVO", 0.95, "muito bom"));

            mockMvc.perform(post("/sentiment").contentType(MediaType.APPLICATION_JSON).content("""
                            {
                                "text": "muito bom"
                            }
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sentiment").value("POSITIVO"))
                    .andExpect(jsonPath("$.score").isNumber())
                    .andExpect(jsonPath("$.text").value("muito bom"));

        }

        @Test
        void shouldReturn200WhenSendingPostToAnalyzeWithNegativeSentiment() throws Exception {
            when(sentimentService.analyzeAndSave(anyString()))
                    .thenReturn(new SentimentResponseDTO("NEGATIVO", 0.95, "muito ruim"));

            mockMvc.perform(post("/sentiment").contentType(MediaType.APPLICATION_JSON).content("""
                            {
                                "text": "muito ruim"
                            }
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sentiment").value("NEGATIVO"))
                    .andExpect(jsonPath("$.score").isNumber())
                    .andExpect(jsonPath("$.text").value("muito ruim"));

        }

        @Test
        void shouldReturn400WhenSendingPostToAnalyzeWithEmptyText() throws Exception {
            mockMvc.perform(post("/sentiment").contentType(MediaType.APPLICATION_JSON).content("""
                            {
                                "text": ""
                            }
                            """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn400WhenSendingPostToAnalyzeWithMissingText() throws Exception {
            mockMvc.perform(post("/sentiment").contentType(MediaType.APPLICATION_JSON).content("""
                            {
                            }
                            """))
                    .andExpect(status().isBadRequest());
        }
    }


    /* Test analyze sentiment for CSV batch */
    @Nested
    @DisplayName("Tests for /sentiment/batch endpoint")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class BatchAnalyzeSentimentTests {
        private Path tempFile;
        private Path customColumnFile;
        private Path emptyFile;
        private Path largeFile;

        @BeforeAll
        void setupCsv() throws Exception {
            // 1. CSV default (column 'text')
            tempFile = Files.createTempFile("test_sentiment", ".csv");
            try (PrintWriter writer = new PrintWriter(new FileWriter(tempFile.toFile()))) {
                writer.println("text");
                writer.println("Eu amo este produto!");
                writer.println("Péssimo, odiei.");
            }

            // 2. CSV with custom column ('comentario')
            customColumnFile = Files.createTempFile("test_custom", ".csv");
            try (PrintWriter pw = new PrintWriter(new FileWriter(customColumnFile.toFile()))) {
                pw.println("comentario");
                pw.println("Texto na coluna customizada");
                pw.println("Mais um comentário");
            }

            // 3. Empty CSV (only header)
            emptyFile = Files.createTempFile("test_empty", ".csv");
            try (PrintWriter pw = new PrintWriter(new FileWriter(emptyFile.toFile()))) {
                pw.println("text");
            }

            // 4. Big CSV (to test limits)
            largeFile = Files.createTempFile("test_large", ".csv");
            try (PrintWriter pw = new PrintWriter(new FileWriter(largeFile.toFile()))) {
                pw.println("text");
                for (int i = 0; i < 150; i++) {
                    pw.println("Linha de teste número " + i);
                }
            }
        }

        @Test
        void shouldReturn200WhenSendingValidCsvFile() throws Exception {
            MockMultipartFile mockFile = getMockMultipartFile(tempFile);

            BatchSentimentResponseDTO response = new BatchSentimentResponseDTO(
                    List.of(
                            new SentimentResponseDTO("POSITIVO", 0.95, "Eu amo este produto!"),
                            new SentimentResponseDTO("NEGATIVO", 0.85, "Péssimo, odiei.")
                    ),
                    2
            );

            when(batchService.processCSV(mockFile, null)).thenReturn(response);

            mockMvc.perform(multipart("/sentiment/batch")
                            .file(mockFile))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.results").isArray())
                    .andExpect(jsonPath("$.results.length()").value(2))
                    .andExpect(jsonPath("$.results[0].sentiment").value("POSITIVO"))
                    .andExpect(jsonPath("$.results[1].sentiment").value("NEGATIVO"))
                    .andExpect(jsonPath("$.totalProcessed").value(2));
        }

        @Test
        void shouldReturn200WhenSendingCsvWithCustomColumn() throws Exception {
            MockMultipartFile mockFile = getMockMultipartFile(customColumnFile);

            BatchSentimentResponseDTO response = new BatchSentimentResponseDTO(
                    List.of(
                            new SentimentResponseDTO("POSITIVO", 0.90, "Texto na coluna customizada"),
                            new SentimentResponseDTO("NEGATIVO", 0.80, "Mais um comentário")
                    ),
                    2
            );

            when(batchService.processCSV(any(), anyString())).thenReturn(response);

            mockMvc.perform(multipart("/sentiment/batch")
                            .file(mockFile)
                            .param("textColumn", "comentario"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.results").isArray())
                    .andExpect(jsonPath("$.results.length()").value(2))
                    .andExpect(jsonPath("$.results[0].sentiment").value("POSITIVO"))
                    .andExpect(jsonPath("$.results[1].sentiment").value("NEGATIVO"))
                    .andExpect(jsonPath("$.totalProcessed").value(2));
        }

        @Test
        void shouldReturn400WhenSendingEmptyCsvFile() throws Exception {
            MockMultipartFile mockFile = getMockMultipartFile(emptyFile);

            when(batchService.processCSV(any(), any())).thenThrow(new IllegalArgumentException("No valid text found in CSV"));

            mockMvc.perform(multipart("/sentiment/batch")
                            .file(mockFile))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn400WhenFileDoesNotHaveCsvExtension() throws Exception {
            MockMultipartFile mockFile = new MockMultipartFile(
                    "file",
                    "invalid.txt",
                    "text/plain",
                    "Some content".getBytes()
            );

            when(batchService.processCSV(any(), any()))
                    .thenThrow(new IllegalArgumentException("File must have .csv extension"));

            mockMvc.perform(multipart("/sentiment/batch")
                            .file(mockFile))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn400WhenNoValidTextFoundInCsv() throws Exception {
            Path invalidFile = Files.createTempFile("test_invalid", ".csv");
            try (PrintWriter pw = new PrintWriter(new FileWriter(invalidFile.toFile()))) {
                pw.println("text");
                pw.println("");
                pw.println("   ");
            }

            MockMultipartFile mockFile = getMockMultipartFile(invalidFile);

            when(batchService.processCSV(any(), any()))
                    .thenThrow(new IllegalArgumentException("No valid text found in CSV"));

            mockMvc.perform(multipart("/sentiment/batch")
                            .file(mockFile))
                    .andExpect(status().isBadRequest());

            Files.deleteIfExists(invalidFile);
        }

        @Test
        void shouldProcessOnlyUpToMaxLinesInLargeCsv() throws Exception {
            MockMultipartFile mockFile = getMockMultipartFile(largeFile);

            // Simula retorno do service com 100 itens (o máximo)
            List<SentimentResponseDTO> responses = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                responses.add(new SentimentResponseDTO("POSITIVO", 0.90, "Linha de teste número " + i));
            }
            BatchSentimentResponseDTO response = new BatchSentimentResponseDTO(responses, 100);

            when(batchService.processCSV(any(), any())).thenReturn(response);

            mockMvc.perform(multipart("/sentiment/batch")
                            .file(mockFile))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.results").isArray())
                    .andExpect(jsonPath("$.results.length()").value(100))
                    .andExpect(jsonPath("$.totalProcessed").value(100));
        }

        @Test
        void shouldReturn400WhenCsvHasInvalidCustomColumn() throws Exception {
            MockMultipartFile mockFile = getMockMultipartFile(tempFile);

            when(batchService.processCSV(any(), anyString()))
                    .thenThrow(new IllegalArgumentException("Column 'nonexistent_column' not found"));

            mockMvc.perform(multipart("/sentiment/batch")
                            .file(mockFile)
                            .param("textColumn", "nonexistent_column"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn500WhenNoFileIsSent() throws Exception {
            mockMvc.perform(multipart("/sentiment/batch"))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        void shouldReturn400WhenSendingAnEmptyFile() throws Exception {
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file",
                    "empty.csv",
                    "text/csv",
                    new byte[0]
            );

            when(batchService.processCSV(any(), any()))
                    .thenThrow(new IllegalArgumentException("CSV file is required"));

            mockMvc.perform(multipart("/sentiment/batch")
                            .file(emptyFile))
                    .andExpect(status().isBadRequest());
        }

        @AfterAll
        void closeCsv() throws Exception {
            Files.deleteIfExists(tempFile);
            Files.deleteIfExists(customColumnFile);
            Files.deleteIfExists(emptyFile);
            Files.deleteIfExists(largeFile);
        }

        // Helper to convert Path files to MockMultipartFile in tests
        private MockMultipartFile getMockMultipartFile(Path path) throws Exception {
            return new MockMultipartFile(
                    "file",
                    path.getFileName().toString(),
                    "text/csv",
                    Files.readAllBytes(path)
            );
        }
    }


    /* Test statistics endpoint */
    @Nested
    @DisplayName("Tests for /sentiment/statistics endpoint")
    class StatisticsTests {
        @Test
        void shouldReturn200WhenGettingStatistics() throws Exception {
            when(statisticsService.getStatistics()).thenReturn(new StatisticsDTO(
                    2,
                    1,
                    1,
                    50.0,
                    50.0,
                    80.0,
                    60.0,
                    60.0,
                    List.of(
                            new DailyStatisticsDTO(LocalDate.of(2026, 1, 7), 1, 0, 1),
                            new DailyStatisticsDTO(LocalDate.of(2026, 1, 8), 0, 1, 1)

                    )
            ));

            mockMvc.perform(get("/sentiment/statistics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(2))
                    .andExpect(jsonPath("$.positive").value(1))
                    .andExpect(jsonPath("$.negative").value(1))
                    .andExpect(jsonPath("$.positivePercentage").value(50.0))
                    .andExpect(jsonPath("$.negativePercentage").value(50.0))
                    .andExpect(jsonPath("$.averageConfidence").value(80.0))
                    .andExpect(jsonPath("$.positiveAverageConfidence").value(60.0))
                    .andExpect(jsonPath("$.negativeAverageConfidence").value(60.0))
                    .andExpect(jsonPath("$.timeline").isArray())
                    .andExpect(jsonPath("$.timeline.length()").value(2))
                    .andExpect(jsonPath("$.timeline[0].date").value("2026-01-07"))
                    .andExpect(jsonPath("$.timeline[0].positive").value(1))
                    .andExpect(jsonPath("$.timeline[0].negative").value(0))
                    .andExpect(jsonPath("$.timeline[0].total").value(1))
                    .andExpect(jsonPath("$.timeline[1].date").value("2026-01-08"))
                    .andExpect(jsonPath("$.timeline[1].positive").value(0))
                    .andExpect(jsonPath("$.timeline[1].negative").value(1))
                    .andExpect(jsonPath("$.timeline[1].total").value(1));
        }

        @Test
        void shouldReturn500WhenStatisticsServiceThrowsException() throws Exception {
            when(statisticsService.getStatistics()).thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(get("/sentiment/statistics"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void shouldReturnZeroStatisticsWhenNoDataExists() throws Exception {
            when(statisticsService.getStatistics()).thenReturn(new StatisticsDTO(
                    0,
                    0,
                    0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    List.of()
            ));

            mockMvc.perform(get("/sentiment/statistics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(0))
                    .andExpect(jsonPath("$.positive").value(0))
                    .andExpect(jsonPath("$.negative").value(0))
                    .andExpect(jsonPath("$.positivePercentage").value(0.0))
                    .andExpect(jsonPath("$.negativePercentage").value(0.0))
                    .andExpect(jsonPath("$.averageConfidence").value(0.0))
                    .andExpect(jsonPath("$.positiveAverageConfidence").value(0.0))
                    .andExpect(jsonPath("$.negativeAverageConfidence").value(0.0))
                    .andExpect(jsonPath("$.timeline").isArray())
                    .andExpect(jsonPath("$.timeline.length()").value(0));
        }
    }

    /* Test history endpoint */
    @Nested
    @DisplayName("Tests for /sentiment/history endpoint")
    class HistoryTests {
        @Test
        void shouldReturn200WhenGettingHistory() throws Exception {
            HistoryItemDTO item1 = new HistoryItemDTO(
                    1L,
                    "Texto positivo",
                    "POSITIVO",
                    0.95,
                    LocalDate.of(2026, 1, 7).atStartOfDay()
            );

            HistoryItemDTO item2 = new HistoryItemDTO(
                    2L,
                    "Texto negativo",
                    "NEGATIVO",
                    0.85,
                    LocalDate.of(2026, 1, 7).atStartOfDay()
            );

            when(historyService.getHistory()).thenReturn(
                    new HistoryItemListDTO(List.of(item1, item2))
            );

            mockMvc.perform(get("/sentiment/history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.historyItemList").isArray())
                    .andExpect(jsonPath("$.historyItemList.length()").value(2))
                    .andExpect(jsonPath("$.historyItemList[0].id").value(1))
                    .andExpect(jsonPath("$.historyItemList[0].textContent").value("Texto positivo"))
                    .andExpect(jsonPath("$.historyItemList[0].sentimentResult").value("POSITIVO"))
                    .andExpect(jsonPath("$.historyItemList[0].confidenceScore").value(0.95))
                    .andExpect(jsonPath("$.historyItemList[0].analyzedAt").exists())
                    .andExpect(jsonPath("$.historyItemList[1].id").value(2))
                    .andExpect(jsonPath("$.historyItemList[1].textContent").value("Texto negativo"))
                    .andExpect(jsonPath("$.historyItemList[1].sentimentResult").value("NEGATIVO"))
                    .andExpect(jsonPath("$.historyItemList[1].confidenceScore").value(0.85))
                    .andExpect(jsonPath("$.historyItemList[1].analyzedAt").exists());
        }

        @Test
        void shouldReturn200WithEmptyListWhenNoHistoryExists() throws Exception {
            when(historyService.getHistory()).thenReturn(
                    new HistoryItemListDTO(List.of())
            );

            mockMvc.perform(get("/sentiment/history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.historyItemList").isArray())
                    .andExpect(jsonPath("$.historyItemList.length()").value(0));
        }

        @Test
        void shoulReturn500WhenHistoryIsUnavailable() throws Exception {
            when(historyService.getHistory()).thenThrow(new RuntimeException("Error retrieving sentiment history"));

            mockMvc.perform(get("/sentiment/history"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void shouldReturn500WhenHistoryServiceThrowsException() throws Exception {
            when(historyService.getHistory()).thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(get("/sentiment/history"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void shouldReturnHistoryOrderedByDateDescending() throws Exception {
            HistoryItemDTO newerItem = new HistoryItemDTO(
                    3L,
                    "Texto mais recente",
                    "POSITIVO",
                    0.90,
                    LocalDate.of(2026, 1, 7).atTime(14, 0)
            );

            HistoryItemDTO olderItem = new HistoryItemDTO(
                    2L,
                    "Texto mais antigo",
                    "NEGATIVO",
                    0.80,
                    LocalDate.of(2026, 1, 6).atTime(10, 0)
            );

            when(historyService.getHistory()).thenReturn(
                    new HistoryItemListDTO(List.of(newerItem, olderItem))
            );

            mockMvc.perform(get("/sentiment/history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.historyItemList").isArray())
                    .andExpect(jsonPath("$.historyItemList.length()").value(2))
                    .andExpect(jsonPath("$.historyItemList[0].id").value(3))
                    .andExpect(jsonPath("$.historyItemList[1].id").value(2));
        }
    }
}
