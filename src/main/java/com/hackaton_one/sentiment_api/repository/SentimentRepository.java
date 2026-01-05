package com.hackaton_one.sentiment_api.repository;

import com.hackaton_one.sentiment_api.model.Sentiment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SentimentRepository extends JpaRepository<Sentiment, Long> {
    
    /**
     * Conta análises por sentimento
     */
    long countBySentimentResult(String sentimentResult);
    
    /**
     * Busca análises por intervalo de datas
     */
    List<Sentiment> findByAnalyzedAtBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * Busca análises recentes ordenadas por data
     */
    List<Sentiment> findTop100ByOrderByAnalyzedAtDesc();
    
    /**
     * Calcula a média de confiança por sentimento
     */
    @Query("SELECT AVG(s.confidenceScore) FROM Sentiment s WHERE s.sentimentResult = :sentiment")
    Double averageConfidenceBySentiment(String sentiment);
    
    /**
     * Calcula a média geral de confiança
     */
    @Query("SELECT AVG(s.confidenceScore) FROM Sentiment s")
    Double averageConfidence();
    
    /**
     * Busca análises agrupadas por dia
     * Usa CAST para compatibilidade com H2 e PostgreSQL
     */
    @Query("SELECT CAST(s.analyzedAt AS DATE) as date, " +
           "COUNT(CASE WHEN s.sentimentResult = 'POSITIVO' THEN 1 END) as positive, " +
           "COUNT(CASE WHEN s.sentimentResult = 'NEGATIVO' THEN 1 END) as negative, " +
           "COUNT(s) as total " +
           "FROM Sentiment s " +
           "WHERE s.analyzedAt >= :startDate " +
           "GROUP BY CAST(s.analyzedAt AS DATE) " +
           "ORDER BY date DESC")
    List<Object[]> findDailyStatistics(LocalDateTime startDate);
}

