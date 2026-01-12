package com.hackaton_one.sentiment_api.service;
 
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import com.hackaton_one.sentiment_api.api.dto.SentimentResponseDTO;
import com.hackaton_one.sentiment_api.api.dto.SentimentResultDTO;
import com.hackaton_one.sentiment_api.exceptions.ModelAnalysisException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
 
import java.io.File;
import java.text.Normalizer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
 
/**
 * Serviço para realizar inferência de análise de sentimento
 * utilizando um modelo ONNX com ONNX Runtime.
 *
 * Responsável por carregar o modelo ONNX, preparar os dados de entrada,
 * executar a inferência e retornar os resultados.
 */
@Slf4j
@Service
public class SentimentService {
    private OrtEnvironment env;
    private OrtSession session;
 
    @Value("${sentiment.model.path:models/sentiment_model.onnx}")
    private String modelPath;
 
    @Getter
    private boolean modelAvailable = false;
 
    private final SentimentPersistenceService persistenceService;
 
    public SentimentService(SentimentPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }
 
    /**
     * Normalizes the input text by removing accents, special characters, and extra whitespace.
     * 
     * This method performs the following operations:
     * 1. Removes diacritical marks (accents) from characters
     * 2. Removes special characters, keeping only alphanumeric and spaces
     * 3. Converts to lowercase
     * 4. Trims leading and trailing whitespace
     * 5. Replaces multiple consecutive spaces with a single space
     * 
     * @param texto the input text to be normalized
     * @return the normalized text, or an empty string if input is null
     */
    private static String normalizeText(String texto) {
        if (texto == null) return "";
        String unaccentedText = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        unaccentedText = unaccentedText.replaceAll("[^a-zA-Z0-9\\s]", "");
        return unaccentedText.toLowerCase().trim().replaceAll("\\s+", " ");
    }
 
    @PostConstruct
    public void init() {
        try {
            log.info("Initializing ONNX Runtime...");
 
            File modelFile = new File(modelPath);
            if (!modelFile.exists()) {
                log.error("CRITICAL: ONNX model file NOT found at: " + modelFile.getAbsolutePath());
                this.modelAvailable = false;
                return;
            }
 
            this.env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT);
 
            this.session = env.createSession(modelPath, opts);
 
            this.modelAvailable = true;
            log.info("ONNX model loaded successfully from disk: " + modelPath);
 
        } catch (Exception e) {
            log.error("Fatal error loading ONNX model: {}", e.getMessage(), e);
            this.modelAvailable = false;
            this.env = null;
            this.session = null;
        }
    }
 
    /**
     * Analisa o sentimento de um texto.
     *
     * @param text Texto a ser analisado
     * @return SentimentResultDTO com previsao e probabilidade
     */
    public SentimentResultDTO analyze(String text) {
        text = normalizeText(text);
 
        String[] inputData = new String[]{ text };
        long[] shape = new long[]{ 1, 1 };
 
        String inputName = session.getInputNames().iterator().next();
 
        try (OnnxTensor tensor = OnnxTensor.createTensor(env, inputData, shape)) {
            Map<String, OnnxTensor> inputs = Collections.singletonMap(inputName, tensor);
 
            try (OrtSession.Result results = session.run(inputs)) {
                String[] labels = (String[]) results.get(0).getValue();
                String previsao = labels[0];
 
                Object probsObj = results.get(1).getValue();
                @SuppressWarnings("unchecked")
                List<ai.onnxruntime.OnnxMap> probsList = (List<ai.onnxruntime.OnnxMap>) probsObj;
                ai.onnxruntime.OnnxMap onnxMap = probsList.get(0);
 
                @SuppressWarnings("unchecked")
                Map<String, Float> mapProbability = (Map<String, Float>) onnxMap.getValue();
 
                float probabilidade = mapProbability.get(previsao);
 
                String previsaoUpper = previsao.toUpperCase().trim();
                if (!previsaoUpper.equals("POSITIVE")
                        && !previsaoUpper.equals("NEGATIVE")
                        && !previsaoUpper.equals("POSITIVO")
                        && !previsaoUpper.equals("NEGATIVO")) {
                    throw new ModelAnalysisException("Modelo retornou sentimento não suportado: " + previsaoUpper);
                }
 
                String sentimentoFinal =
                        (previsaoUpper.equals("POSITIVE") || previsaoUpper.equals("POSITIVO"))
                                ? "POSITIVO"
                                : "NEGATIVO";
 
                return new SentimentResultDTO(sentimentoFinal, probabilidade);
            }
        } catch (Exception e){
            throw new ModelAnalysisException("Erro na inferência: " + e.getMessage(), e);
        }
    }
 
    /**
     * Analisa o sentimento de um texto e persiste o resultado no banco de dados.
     *
     * @param text Texto a ser analisado
     * @return SentimentResponseDTO pronto para ser retornado pela API
     */
    public SentimentResponseDTO analyzeAndSave(String text) {
        SentimentResultDTO result = analyze(text);
 
        String sentiment = result.previsao().toUpperCase();
        double score = result.probabilidade();
 
        try {
            persistenceService.saveSentiment(text, sentiment, score);
        } catch (Exception e) {
            log.warn("Erro ao salvar análise no banco (continuando): {}", e.getMessage());
        }
 
        return new SentimentResponseDTO(sentiment, score, text);
    }
 
    @PreDestroy
    public void cleanup(){
        try {
            if (session != null) session.close();
            if (env != null) env.close();
        } catch (Exception e){
            log.error("Error during ONNX Runtime cleanup: {}", e.getMessage(), e);
        }
    }
}