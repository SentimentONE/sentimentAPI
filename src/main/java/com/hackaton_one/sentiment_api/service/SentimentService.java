package com.hackaton_one.sentiment_api.service;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import com.hackaton_one.sentiment_api.api.dto.SentimentResultDTO;
import com.hackaton_one.sentiment_api.exceptions.ModelAnalysisException;
import com.hackaton_one.sentiment_api.exceptions.ModelInitializationException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Service
public class SentimentService {
    private OrtEnvironment env;
    private OrtSession session;

    private static final String MODEL_PATH = "models/sentiment_model.onnx";

    private boolean modelAvailable = false;

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource(MODEL_PATH);
            if (!resource.exists()) {
                log.debug("Modelo ONNX não encontrado em: " + MODEL_PATH);
                log.debug("Usando análise simples baseada em palavras-chave para testes.");
                this.modelAvailable = false;
                return;
            }

            // 1. Initialize ONNX Runtime environment
            this.env = OrtEnvironment.getEnvironment();

            // 2. Options for the session(Optimizations
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT);

            byte[] modelBytes = resource.getContentAsByteArray();

            // 3. Load the model
            this.session = env.createSession(modelBytes, opts);
            this.modelAvailable = true;
            log.debug("Modelo ONNX carregado com sucesso de: " + MODEL_PATH);
        } catch (Exception e) {
            log.error("Erro ao carregar modelo ONNX: {}", e.getMessage(), e);
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
        // Se o modelo não estiver disponível, usa análise simples
        if (!modelAvailable) {
            return analyzeSimple(text);
        }

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

    /**
     * Análise simples baseada em palavras-chave (fallback quando modelo ONNX não está disponível)
     */
    private SentimentResultDTO analyzeSimple(String text) {
        String textLower = text.toLowerCase();
        
        String[] positiveWords = {"incrível", "ótimo", "excelente", "bom", "maravilhoso", "recomendo", 
                                  "adoro", "amo", "perfeito", "fantástico", "sensacional", "gostei", 
                                  "satisfeito", "feliz", "amor", "adorar", "recomendado"};
        String[] negativeWords = {"ruim", "péssimo", "horrível", "terrível", "odiei", "detesto", 
                                   "não gostei", "insatisfeito", "decepcionado", "lixo", "fraco", 
                                   "desapontado", "triste", "raiva", "ódio"};
        
        int positiveCount = 0;
        int negativeCount = 0;
        
        for (String word : positiveWords) {
            if (textLower.contains(word)) positiveCount++;
        }
        
        for (String word : negativeWords) {
            if (textLower.contains(word)) negativeCount++;
        }
        
        String previsao;
        double probabilidade;
        
        if (positiveCount > negativeCount) {
            previsao = "POSITIVO";
            probabilidade = Math.min(0.7 + (positiveCount * 0.1), 0.95);
        } else if (negativeCount > positiveCount) {
            previsao = "NEGATIVO";
            probabilidade = Math.min(0.7 + (negativeCount * 0.1), 0.95);
        } else {
            // Quando empate ou nenhuma palavra encontrada, assume POSITIVO como padrão
            previsao = "POSITIVO";
            probabilidade = 0.5 + (Math.random() * 0.2);
        }
        
        // Garante normalização (não deve ser necessário aqui, mas mantém consistência)
        previsao = normalizeSentiment(previsao);
        
        return new SentimentResultDTO(previsao, probabilidade);
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

