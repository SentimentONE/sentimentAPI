package com.hackaton_one.sentiment_api.integration;

import com.hackaton_one.sentiment_api.api.controller.SentimentController;
import com.hackaton_one.sentiment_api.api.dto.DailyStatisticsDTO;
import com.hackaton_one.sentiment_api.api.dto.SentimentResultDTO;
import com.hackaton_one.sentiment_api.api.dto.StatisticsDTO;
import com.hackaton_one.sentiment_api.repository.SentimentRepository;
import com.hackaton_one.sentiment_api.service.BatchService;
import com.hackaton_one.sentiment_api.service.SentimentPersistenceService;
import com.hackaton_one.sentiment_api.service.SentimentService;
import com.hackaton_one.sentiment_api.service.StatisticsService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SentimentController.class)
@Import(BatchService.class)
public class SentimentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SentimentPersistenceService sentimentPersistenceService;

    @MockitoBean
    private SentimentService sentimentService;

    @MockitoBean
    private StatisticsService statisticsService;

    @MockitoBean
    private SentimentRepository sentimentRepository;

    /* Test analyze sentiment single text endpoint */
    @Nested
    @DisplayName("Tests for /sentiment endpoint")
    class AnalyzeSentimentTests {
        @Test
        void shouldReturn200WhenSendingPostToAnalyzeWithPositiveSentiment() throws Exception {
            when(sentimentService.analyze(anyString())).thenReturn(new SentimentResultDTO("POSITIVO", 0.95));

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
            when(sentimentService.analyze(anyString())).thenReturn(new SentimentResultDTO("NEGATIVO", 0.95));

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

        @Test
        void shouldReturn200EvenWhenPersistenceServiceThrowsException() throws Exception {
            when(sentimentService.analyze(anyString())).thenReturn(new SentimentResultDTO("POSITIVO", 0.95));
            when(sentimentPersistenceService.saveSentiment(anyString(), anyString(), anyDouble()))
                    .thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(post("/sentiment").contentType(MediaType.APPLICATION_JSON).content("""
                            {
                                "text": "teste de erro"
                            }
                            """))
                    .andExpect(status().isOk()) // Espera 200 OK pois o erro é tratado (logado e ignorado)
                    .andExpect(jsonPath("$.sentiment").value("POSITIVO"))
                    .andExpect(jsonPath("$.score").value(0.95));
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
            when(sentimentService.analyze(anyString()))
                    .thenReturn(new SentimentResultDTO("POSITIVO", 0.95))
                    .thenReturn(new SentimentResultDTO("NEGATIVO", 0.85));

            MockMultipartFile mockFile = getMockMultipartFile(tempFile);

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
            when(sentimentService.analyze(anyString()))
                    .thenReturn(new SentimentResultDTO("POSITIVO", 0.90))
                    .thenReturn(new SentimentResultDTO("NEGATIVO", 0.80));

            MockMultipartFile mockFile = getMockMultipartFile(customColumnFile);

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

            mockMvc.perform(multipart("/sentiment/batch")
                            .file(mockFile))
                    .andExpect(status().isBadRequest());

            Files.deleteIfExists(invalidFile);
        }

        @Test
        void shouldProcessOnlyUpToMaxLinesInLargeCsv() throws Exception {
            when(sentimentService.analyze(anyString()))
                    .thenReturn(new SentimentResultDTO("POSITIVO", 0.90));

            MockMultipartFile mockFile = getMockMultipartFile(largeFile);

            mockMvc.perform(multipart("/sentiment/batch")
                            .file(mockFile))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.results").isArray())
                    .andExpect(jsonPath("$.results.length()").value(100)) // Assuming maxLines is set to 100
                    .andExpect(jsonPath("$.totalProcessed").value(100));
        }

        @Test
        void shouldReturn400WhenCsvHasInvalidCustomColumn() throws Exception {
            MockMultipartFile mockFile = getMockMultipartFile(tempFile);

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

            mockMvc.perform(multipart("/sentiment/batch")
                            .file(emptyFile))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn200EvenWhenPersistenceServiceThrowsExceptionDuringBatchProcessing() throws Exception {
            when(sentimentService.analyze(anyString()))
                    .thenReturn(new SentimentResultDTO("POSITIVO", 0.95))
                    .thenReturn(new SentimentResultDTO("NEGATIVO", 0.85));

            when(sentimentPersistenceService.saveSentiment(anyString(), anyString(), anyDouble()))
                    .thenThrow(new RuntimeException("Database error"));

            MockMultipartFile mockFile = getMockMultipartFile(tempFile);

            mockMvc.perform(multipart("/sentiment/batch")
                            .file(mockFile))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.results").isArray())
                    .andExpect(jsonPath("$.results.length()").value(2))
                    .andExpect(jsonPath("$.results[0].sentiment").value("POSITIVO"))
                    .andExpect(jsonPath("$.results[1].sentiment").value("NEGATIVO"))
                    .andExpect(jsonPath("$.totalProcessed").value(2));
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
                            new DailyStatisticsDTO(LocalDate.of(2026,1,7), 1, 0, 1),
                            new DailyStatisticsDTO(LocalDate.of(2026,1,8), 0, 1, 1)

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
    // TODO: Implement history tests
}
