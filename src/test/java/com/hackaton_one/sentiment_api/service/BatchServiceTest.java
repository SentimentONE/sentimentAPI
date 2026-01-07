package com.hackaton_one.sentiment_api.service;

import com.hackaton_one.sentiment_api.api.dto.BatchSentimentResponseDTO;
import com.hackaton_one.sentiment_api.api.dto.SentimentResultDTO;
import com.hackaton_one.sentiment_api.exceptions.CsvProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BatchService Unit Tests")
class BatchServiceTest {

    @Mock
    private SentimentService sentimentService;

    @Mock
    private SentimentPersistenceService persistenceService;

    @InjectMocks
    private BatchService batchService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(batchService, "maxLines", 100);
    }

    @Test
    @DisplayName("Should throw exception when file is empty")
    void shouldThrowExceptionWhenFileIsEmpty() {
        MultipartFile emptyFile = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                new byte[0]
        );

        assertThrows(IllegalArgumentException.class, () -> batchService.validateCSVFile(emptyFile));
    }

    @Test
    @DisplayName("Should throw exception when file is not CSV")
    void shouldThrowExceptionWhenFileIsNotCSV() {
        MultipartFile txtFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Some content".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () -> batchService.validateCSVFile(txtFile));
    }

    @Test
    @DisplayName("Should accept valid CSV file")
    void shouldAcceptValidCSVFile() {
        MultipartFile csvFile = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                "text\nSample text".getBytes()
        );

        assertDoesNotThrow(() -> batchService.validateCSVFile(csvFile));
    }

    @Test
    @DisplayName("Should throw exception when filename is null")
    void shouldThrowExceptionWhenFilenameIsNull() {
        MultipartFile fileWithNullName = new MockMultipartFile(
                "file",
                null,
                "text/csv",
                "content".getBytes()
        );

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> {
            batchService.validateCSVFile(fileWithNullName);
        });
    }

    @Test
    @DisplayName("Should process CSV with valid data")
    void shouldProcessCSVWithValidData() {
        String csvContent = "text\nTexto positivo\nTexto negativo";
        byte[] csvBytes = csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        MultipartFile csvFile = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                csvBytes
        );

        when(sentimentService.analyze("Texto positivo"))
                .thenReturn(new SentimentResultDTO("POSITIVO", 0.95));
        when(sentimentService.analyze("Texto negativo"))
                .thenReturn(new SentimentResultDTO("NEGATIVO", 0.85));

        BatchSentimentResponseDTO result = batchService.processCSV(csvFile, null);

        assertNotNull(result);
        assertEquals(2, result.totalProcessed());
        assertEquals(2, result.results().size());
        verify(sentimentService, times(2)).analyze(anyString());
    }

    @Test
    @DisplayName("Should throw exception when no valid text found")
    void shouldThrowExceptionWhenNoValidTextFound() {
        String csvContent = "text\n\n   \n";
        MultipartFile csvFile = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                csvContent.getBytes()
        );

        assertThrows(IllegalArgumentException.class, () -> batchService.processCSV(csvFile, null));
    }

    @Test
    @DisplayName("Should process CSV with custom column")
    void shouldProcessCSVWithCustomColumn() {
        String csvContent = "id,comentario\n1,Texto customizado";
        MultipartFile csvFile = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        when(sentimentService.analyze("Texto customizado"))
                .thenReturn(new SentimentResultDTO("POSITIVO", 0.90));

        BatchSentimentResponseDTO result = batchService.processCSV(csvFile, "comentario");

        assertNotNull(result);
        assertEquals(1, result.totalProcessed());
        verify(sentimentService, times(1)).analyze("Texto customizado");
    }

    @Test
    @DisplayName("Should throw exception when custom column not found")
    void shouldThrowExceptionWhenCustomColumnNotFound() {
        String csvContent = "text\nSample text";
        MultipartFile csvFile = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                csvContent.getBytes()
        );

        assertThrows(CsvProcessingException.class, () -> batchService.processCSV(csvFile, "nonexistent"));
    }

    @Test
    @DisplayName("Should handle persistence errors gracefully")
    void shouldHandlePersistenceErrorsGracefully() {
        String csvContent = "text\nTexto de teste";
        MultipartFile csvFile = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        when(sentimentService.analyze("Texto de teste"))
                .thenReturn(new SentimentResultDTO("POSITIVO", 0.95));

        doThrow(new RuntimeException("Database error"))
                .when(persistenceService)
                .saveSentiment(anyString(), anyString(), anyDouble());

        BatchSentimentResponseDTO result = batchService.processCSV(csvFile, null);

        assertNotNull(result);
        assertEquals(1, result.totalProcessed());
    }

    @Test
    @DisplayName("Should parse CSV with quoted values")
    void shouldParseCSVWithQuotedValues() {
        String csvContent = "text\n\"Texto com vírgula, aqui\"";
        MultipartFile csvFile = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        when(sentimentService.analyze("Texto com vírgula, aqui"))
                .thenReturn(new SentimentResultDTO("POSITIVO", 0.90));

        BatchSentimentResponseDTO result = batchService.processCSV(csvFile, null);

        assertNotNull(result);
        assertEquals(1, result.totalProcessed());
    }

    @Test
    @DisplayName("Should respect max lines limit")
    void shouldRespectMaxLinesLimit() {
        StringBuilder csvContent = new StringBuilder("text\n");
        for (int i = 0; i < 150; i++) {
            csvContent.append("Linha ").append(i).append("\n");
        }

        MultipartFile csvFile = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                csvContent.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        when(sentimentService.analyze(anyString()))
                .thenReturn(new SentimentResultDTO("POSITIVO", 0.90));

        BatchSentimentResponseDTO result = batchService.processCSV(csvFile, null);

        assertNotNull(result);
        assertTrue(result.totalProcessed() <= 100); // maxLines configurado como 100
    }
}

