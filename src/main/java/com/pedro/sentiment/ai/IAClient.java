package com.pedro.sentiment.ai;

import java.util.List;
import java.util.Objects;

public interface IAClient {
    Result analyze(String text);

    /** Pontuação por aspecto (ex.: "geral", "comunicacao"). */
    final class AspectScore {
        private final String aspect;
        private final double positive;
        private final double negative;

        public AspectScore(String aspect, double positive, double negative) {
            this.aspect = aspect;
            this.positive = positive;
            this.negative = negative;
        }

        public String getAspect()   { return aspect; }
        public double getPositive() { return positive; }
        public double getNegative() { return negative; }
    }

    /** Resultado da análise — compatível com a versão antiga e com a nova. */
    final class Result {
        // Campos antigos (mantidos)
        private final String sentiment;   // POSITIVE | NEGATIVE | NEUTRAL | MIXED
        private final double score;       // 0.0..1.0
        private final String summary;     // resumo curto
        private final String reason;      // justificativa/explicação curta

        // Novos campos
        private final List<AspectScore> aspects; // pode ser List.of()
        private final String provider;           // ex.: "HuggingFace: model-x" | "MockClient"

        /** Construtor antigo (compatibilidade): mantém tudo funcionando. */
        public Result(String sentiment, double score, String summary, String reason) {
            this(sentiment, score, summary, reason, List.of(), "");
        }

        /** Construtor novo (com aspects e provider). */
        public Result(String sentiment, double score, String summary, String reason,
                      List<AspectScore> aspects, String provider) {
            this.sentiment = sentiment;
            this.score = score;
            this.summary = summary;
            this.reason  = reason;
            this.aspects = aspects == null ? List.of() : List.copyOf(aspects);
            this.provider = provider == null ? "" : provider;
        }

        // Getters (mantêm a API atual)
        public String getSentiment() { return sentiment; }
        public double getScore()     { return score; }
        public String getSummary()   { return summary; }
        public String getReason()    { return reason; }
        public List<AspectScore> getAspects() { return aspects; }
        public String getProvider()  { return provider; }

        // (Opcional) helpers de igualdade/depuração
        @Override public String toString() {
            return "Result{sentiment='%s', score=%s, summary='%s', reason='%s', aspects=%s, provider='%s'}"
                    .formatted(sentiment, score, summary, reason, aspects, provider);
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Result r)) return false;
            return Double.compare(r.score, score) == 0
                    && Objects.equals(sentiment, r.sentiment)
                    && Objects.equals(summary, r.summary)
                    && Objects.equals(reason, r.reason)
                    && Objects.equals(aspects, r.aspects)
                    && Objects.equals(provider, r.provider);
        }
        @Override public int hashCode() {
            return Objects.hash(sentiment, score, summary, reason, aspects, provider);
        }
    }
}
