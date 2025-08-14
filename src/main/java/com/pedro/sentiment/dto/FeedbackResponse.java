package com.pedro.sentiment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado da análise de sentimento.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeedbackResponse {

    @Schema(description = "Rótulo principal do sentimento (POSITIVE, NEGATIVE, NEUTRAL, MIXED).",
            example = "POSITIVE")
    private String sentiment;

    @Schema(description = "Força/Confiança do sentimento (0..1).",
            example = "0.87")
    private double score;

    @Schema(description = "Resumo curto do resultado.",
            example = "Avaliação positiva predominante.")
    private String summary;

    @Schema(description = "Motivo/assunto predominante extraído do texto.",
            example = "tempo de espera")
    private String reason;

    @Schema(description = "Identificação do provedor/modelo utilizado.",
            example = "HuggingFace: cardiffnlp/twitter-xlm-roberta-base-sentiment")
    private String provider;

    @Schema(description = "Área foco de melhoria derivada do feedback.",
            example = "performance")
    private String improvementArea;

    @Schema(description = "Sugestão objetiva de melhoria para a área identificada.",
            example = "Otimizar endpoints críticos e habilitar cache onde viável.")
    private String improvement;

    @Schema(description = "Carimbo de data/hora da análise (ISO-8601).",
            example = "2025-08-14T02:10:23.123-03:00")
    private String timestamp;

    public FeedbackResponse() {}

    public FeedbackResponse(String sentiment, double score, String summary) {
        this.sentiment = sentiment;
        this.score = score;
        this.summary = summary;
    }

    public FeedbackResponse(String sentiment, double score, String summary, String reason, String provider) {
        this.sentiment = sentiment;
        this.score = score;
        this.summary = summary;
        this.reason = reason;
        this.provider = provider;
    }

    public String getSentiment() { return sentiment; }
    public void setSentiment(String sentiment) { this.sentiment = sentiment; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getImprovementArea() { return improvementArea; }
    public void setImprovementArea(String improvementArea) { this.improvementArea = improvementArea; }

    public String getImprovement() { return improvement; }
    public void setImprovement(String improvement) { this.improvement = improvement; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
