package com.hackaton_one.sentiment_api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_sentiments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sentiment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O texto para análise não pode estar vazio")
    @Size(min = 5, max = 5000, message = "O texto deve ter entre 5 e 5000 caracteres")
    @Column(nullable = false, length = 5000)
    private String textContent;

    // Ex: "POSITIVO", "NEGATIVO"
    @NotBlank(message = "O resultado da análise é obrigatório")
    private String sentimentResult;

    // Ex: 0.98 (98% de confiança)
    @Min(value = 0, message = "A pontuação deve ser no mínimo 0")
    @Max(value = 1, message = "A pontuação deve ser no máximo 1")
    private Double confidenceScore;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    // Método executado automaticamente antes de salvar no banco
    @PrePersist
    public void prePersist() {
        this.analyzedAt = LocalDateTime.now();
    }
}