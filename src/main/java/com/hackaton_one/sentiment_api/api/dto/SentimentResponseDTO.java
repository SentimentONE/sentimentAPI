package com.hackaton_one.sentiment_api.api.dto;

/**
 * Resposta de saída da análise de sentimento.
 *
 * Exemplo de resposta 200 OK:
 * {
 *   "sentiment": "POSITIVE",
 *   "score": 0.87,
 *   "text": "Este produto é muito bom!"
 * }
 */
public class SentimentResponseDTO {

    private String sentiment;
    private double score;
    private String text;

    public SentimentResponseDTO() {
    }

    public SentimentResponseDTO(String sentiment, double score, String text) {
        this.sentiment = sentiment;
        this.score = score;
        this.text = text;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}

