package com.pedro.sentiment.analysis;

import java.util.ArrayList;
import java.util.List;

public class StrengthsImprovementsExtractor {

    private static final String[] STRENGTH_KEYWORDS = {
            "bom", "ótimo", "excelente", "eficiente", "rápido", "claro", "organizado", "responsável"
    };

    private static final String[] IMPROVEMENT_KEYWORDS = {
            "melhorar", "demora", "lento", "falha", "problema", "falta", "dificuldade"
    };

    public static ExtractionResult extract(String text) {
        String lower = text.toLowerCase();
        List<String> strengths = new ArrayList<>();
        List<String> improvements = new ArrayList<>();

        for (String keyword : STRENGTH_KEYWORDS) {
            if (lower.contains(keyword)) {
                strengths.add(keyword);
            }
        }
        for (String keyword : IMPROVEMENT_KEYWORDS) {
            if (lower.contains(keyword)) {
                improvements.add(keyword);
            }
        }

        return new ExtractionResult(strengths, improvements);
    }

    public record ExtractionResult(List<String> strengths, List<String> improvements) {}
}
