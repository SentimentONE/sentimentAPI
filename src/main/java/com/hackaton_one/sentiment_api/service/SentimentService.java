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

    @PostConstruct
    public void init() {
        try {
            log.info("Initializing ONNX Runtime...");

            // 1. Validate file existence on disk
            File modelFile = new File(modelPath);
            if (!modelFile.exists()) {
                log.error("CRITICAL: ONNX model file NOT found at: " + modelFile.getAbsolutePath());
                log.error("The application requires the model file at this specific path to run efficiently.");
                this.modelAvailable = false;
                return;
            }

            // 2. Initialize Environment
            this.env = OrtEnvironment.getEnvironment();

            // 3. Set Session Options
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT);

            // 4. Load Model directly from Disk (Zero-Copy / Memory Mapped)
            // This is crucial for low-RAM environments. It avoids loading a huge byte[] into Java Heap.
            this.session = env.createSession(modelPath, opts);

            this.modelAvailable = true;
            log.info("ONNX model loaded successfully from disk: " + modelPath);

        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Unsupported model IR version")) {
                log.error("Version mismatch: The ONNX model requires a newer ONNX Runtime or needs to be converted.");
            }
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
        String[] inputData = new String[]{ text };
        long[] shape = new long[]{ 1, 1 };

        String inputName = session.getInputNames().iterator().next();

        try (OnnxTensor tensor = OnnxTensor.createTensor(env, inputData, shape)) {
            Map<String, OnnxTensor> inputs = Collections.singletonMap(inputName, tensor);

            // 5. Run inference
            try (OrtSession.Result results = session.run(inputs)) {
                // 6. Extract output
                String[] labels = (String[]) results.get(0).getValue();
                String previsao = labels[0];

                // Get the probability map - ONNX Runtime returns OnnxSequence containing OnnxMaps
                Object probsObj = results.get(1).getValue();

                // Convert to List of OnnxMap
                @SuppressWarnings("unchecked")
                List<ai.onnxruntime.OnnxMap> probsList = (List<ai.onnxruntime.OnnxMap>) probsObj;

                // Get the first map
                ai.onnxruntime.OnnxMap onnxMap = probsList.get(0);

                // Convert OnnxMap to java.util.Map using the getValue method
                @SuppressWarnings("unchecked")
                Map<String, Float> mapProbability = (Map<String, Float>) onnxMap.getValue();

                float probabilidade = mapProbability.get(previsao);

                // Valida o sentimento retornado pelo modelo
                String previsaoUpper = previsao.toUpperCase().trim();

                // Aceita apenas POSITIVE / NEGATIVE ou POSITIVO / NEGATIVO
                if (!previsaoUpper.equals("POSITIVE")
                        && !previsaoUpper.equals("NEGATIVE")
                        && !previsaoUpper.equals("POSITIVO")
                        && !previsaoUpper.equals("NEGATIVO")) {

                    log.warn("Sentimento inesperado retornado pelo modelo: {}", previsaoUpper);
                    throw new ModelAnalysisException(
                            "Modelo retornou sentimento não suportado: " + previsaoUpper
                    );
                }

                // Padroniza o sentimento para português
                String sentimentoFinal =
                        (previsaoUpper.equals("POSITIVE") || previsaoUpper.equals("POSITIVO"))
                                ? "POSITIVO"
                                : "NEGATIVO";

                return new SentimentResultDTO(sentimentoFinal, probabilidade);

            } catch (Exception e){
                log.error("Failed to run inference: {}", e.getMessage(), e);
                throw new ModelAnalysisException("Failed to run inference: " + e.getMessage(), e);
            }
        } catch (Exception e){
            log.error("Failed to prepare tensor for inference: {}", e.getMessage(), e);
            throw new ModelAnalysisException("Failed to prepare tensor for inference: " + e.getMessage(), e);
        }
    }

    /**
     * Analisa o sentimento de um texto e persiste o resultado no banco de dados.
     * Este método encapsula toda a lógica de negócio, incluindo:
     * - Análise do sentimento
     * - Normalização do resultado
     * - Persistência no banco de dados
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
