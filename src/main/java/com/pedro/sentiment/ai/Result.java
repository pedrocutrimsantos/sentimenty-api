package com.pedro.sentiment.ai;

/**
 * Representa o resultado da análise de sentimento.
 */
public class Result {

    private String sentiment;
    private double score;
    private String summary;
    private String reason;

    public Result() {
    }

    public Result(String sentiment, double score, String summary, String reason) {
        this.sentiment = sentiment;
        this.score = score;
        this.summary = summary;
        this.reason = reason;
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

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
