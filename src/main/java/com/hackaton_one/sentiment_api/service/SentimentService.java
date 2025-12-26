package com.hackaton_one.sentiment_api.service;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import com.hackaton_one.sentiment_api.api.dto.SentimentResultDTO;
import com.hackaton_one.sentiment_api.exceptions.ModelAnalysisException;
import com.hackaton_one.sentiment_api.exceptions.ModelInitializationException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

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
@Service
public class SentimentService {
    private OrtEnvironment env;
    private OrtSession session;

    private static final String MODEL_PATH = "models/sentiment_model.onnx";

    @PostConstruct
    public void init() {
        try {
            // 1. Initialize ONNX Runtime environment
            this.env = OrtEnvironment.getEnvironment();

            // 2. Options for the session(Optimizations
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT);

            byte[] modelBytes = new ClassPathResource(MODEL_PATH).getContentAsByteArray();

            // 3. Load the model
            this.session = env.createSession(modelBytes, opts);
        } catch (Exception e) {
            throw new ModelInitializationException("Failed to load the ONNX model: " + e.getMessage(), e);
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

                @SuppressWarnings("unchecked")
                List<Map<String, Float>> probsList = (List<Map<String, Float>>) results.get(1).getValue();

                Map<String, Float> mapProbability = probsList.get(0);
                float probabilidade = mapProbability.get(previsao);

                return new SentimentResultDTO(previsao, probabilidade);
            } catch (Exception e){
                throw new ModelAnalysisException("Failed to run inference: " + e.getMessage(), e);
            }
        } catch (Exception e){
            throw new ModelAnalysisException("Failed to prepare tensor for inference: " + e.getMessage(), e);
        }
    }

    @PreDestroy
    public void cleanup(){
        try {
            if (session != null) session.close();
            if (env != null) env.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}

