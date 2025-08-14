package com.pedro.sentiment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Resultado da análise de peer feedback (forças, melhorias e aspectos).")
public class PeerFeedbackResponse {

    // ---- Tipos internos ----
    public static class AspectScore {
        public String aspect;
        public double positive;
        public double negative;

        public AspectScore() {}
        public AspectScore(String aspect, double positive, double negative) {
            this.aspect = aspect;
            this.positive = positive;
            this.negative = negative;
        }
    }

    public static class Strength {
        public String aspect;
        public String evidence;

        public Strength() {}
        public Strength(String aspect, String evidence) {
            this.aspect = aspect;
            this.evidence = evidence;
        }
    }

    public static class Improvement {
        public String aspect;
        public String suggestion;
        public String evidence; // opcional

        public Improvement() {}
        public Improvement(String aspect, String suggestion) {
            this.aspect = aspect;
            this.suggestion = suggestion;
        }
        public Improvement(String aspect, String suggestion, String evidence) {
            this.aspect = aspect;
            this.suggestion = suggestion;
            this.evidence = evidence;
        }
    }

    // ---- Campos do payload ----
    public String subjectId;
    public String sentiment;
    public double score;
    public String summary;

    public List<Strength> strengths;
    public List<Improvement> improvements;
    public List<AspectScore> aspects;

    public String provider;
    public String timestamp;
}
