package com.hackaton_one.sentiment_api.service;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import com.hackaton_one.sentiment_api.api.dto.SentimentResultDTO;
import com.hackaton_one.sentiment_api.exceptions.ModelAnalysisException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
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

    private boolean modelAvailable = false;

    /**
     * Verifica se o modelo ONNX está disponível e carregado.
     *
     * @return true se o modelo está disponível, false caso contrário
     */
    public boolean isModelAvailable() {
        return modelAvailable;
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
     * Normaliza o sentimento retornado pelo modelo.
     * Converte NEUTRO/NEUTRAL para POSITIVO, mantendo apenas POSITIVO e NEGATIVO.
     * 
     * @param previsao Sentimento retornado pelo modelo
     * @return Sentimento normalizado (POSITIVO ou NEGATIVO)
     */
    private String normalizeSentiment(String previsao) {
        if (previsao == null) {
            return "POSITIVO";
        }
        
        String previsaoUpper = previsao.toUpperCase().trim();
        
        // Se for NEUTRO ou NEUTRAL, converte para POSITIVO
        if (previsaoUpper.equals("NEUTRO") || previsaoUpper.equals("NEUTRAL")) {
            return "POSITIVO";
        }
        
        // Mantém POSITIVO, POSITIVE, NEGATIVO, NEGATIVE
        if (previsaoUpper.equals("POSITIVE")) {
            return "POSITIVO";
        }
        if (previsaoUpper.equals("NEGATIVE")) {
            return "NEGATIVO";
        }
        
        // Se já estiver em português, retorna como está (ou converte para maiúsculas)
        if (previsaoUpper.equals("POSITIVO") || previsaoUpper.equals("NEGATIVO")) {
            return previsaoUpper;
        }
        
        // Por padrão, assume POSITIVO
        return "POSITIVO";
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
                
                // Normaliza o sentimento (converte NEUTRO/NEUTRAL para POSITIVO)
                String previsaoNormalizada = normalizeSentiment(previsao);
                
                // Se foi convertido de NEUTRO para POSITIVO, ajusta a probabilidade
                if (!previsaoNormalizada.equals(previsao.toUpperCase().trim())) {
                    // Se era NEUTRO, mantém a probabilidade original ou ajusta levemente
                    probabilidade = (float) Math.max(0.5, probabilidade);
                }

                return new SentimentResultDTO(previsaoNormalizada, probabilidade);
            } catch (Exception e){
                log.error("Failed to run inference: {}", e.getMessage(), e);
                throw new ModelAnalysisException("Failed to run inference: " + e.getMessage(), e);
            }
        } catch (Exception e){
            log.error("Failed to prepare tensor for inference: {}", e.getMessage(), e);
            throw new ModelAnalysisException("Failed to prepare tensor for inference: " + e.getMessage(), e);
        }
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
