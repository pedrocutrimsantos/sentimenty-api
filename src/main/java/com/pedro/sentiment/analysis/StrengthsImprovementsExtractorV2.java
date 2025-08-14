package com.pedro.sentiment.analysis;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class StrengthsImprovementsExtractorV2 {

    public record StrengthEvidence(String aspect, String evidence) {}
    public record ImprovementEvidence(String aspect, String evidence) {}

    private static final Pattern P_CONTRASTE = Pattern.compile(
            "\\b(contudo|por[eé]m|porem|no entanto|todavia|entretanto)\\b", Pattern.CASE_INSENSITIVE);

    private static final String[] A_ARQUITETURA_POS = { "design", "arquitetur", "pareament" };
    private static final String[] A_PROCESSO_NEG = { "pr", "pull request", "revis", "review", "demor", "atras", "entrega" };

    private StrengthsImprovementsExtractorV2() {}

    public static Result extract(String originalText) {
        String text = normalize(originalText);

        List<StrengthEvidence> strengths = new ArrayList<>();
        List<ImprovementEvidence> improvements = new ArrayList<>();

        if (containsAny(text, A_ARQUITETURA_POS)) {
            strengths.add(new StrengthEvidence("arquitetura",
                    snippet(originalText, "(design|pareament\\w+)")));
        }

        if (containsAny(text, A_PROCESSO_NEG)) {
            improvements.add(new ImprovementEvidence("processo/fluxo de PR",
                    snippet(originalText, "(PRs?|pull request|revis[aã]o|review|demor\\w+|atras\\w+|entrega)")));
        }

        var m = P_CONTRASTE.matcher(text);
        if (m.find()) {
            String tail = originalTail(originalText, m.start());
            if (!tail.isBlank() && improvements.isEmpty()) {
                improvements.add(new ImprovementEvidence("processo/fluxo de PR", tail.trim()));
            }
        }

        return new Result(strengths, improvements);
    }

    public record Result(List<StrengthEvidence> strengths, List<ImprovementEvidence> improvements) {}

    private static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT);
        n = n.replaceAll("\\s+"," ").trim();
        return n;
    }
    private static boolean containsAny(String text, String[] keys) {
        for (String k : keys) if (text.contains(k)) return true;
        return false;
    }
    private static String snippet(String original, String regex) {
        var p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        var m = p.matcher(original);
        if (m.find()) {
            int start = Math.max(0, original.lastIndexOf('.', m.start()) + 1);
            int end = original.indexOf('.', m.end());
            if (end < 0) end = original.length();
            return original.substring(start, end).trim();
        }
        return original.length() > 140 ? original.substring(0, 140) + "..." : original;
    }
    private static String originalTail(String original, int contrastStartIdxOnNormalized) {
        String[] split = original.split("(?i)\\b(contudo|por[ée]m|porem|no entanto|todavia|entretanto)\\b", 2);
        return split.length == 2 ? split[1] : "";
    }
}
