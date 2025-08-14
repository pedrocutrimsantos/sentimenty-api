package com.pedro.sentiment.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockClient implements IAClient {

    // Vocabulário (sem acentos/caixa)
    private static final Set<String> POS = Set.of(
            "excelente","muito bom","bom","boa","otimo","otima","seguro","eficiente",
            "funcionou","funciona","parabens","gostei","satisfeito","recomendo","perfeito","adorei","amei"
    );
    private static final Set<String> NEG = Set.of(
            "ruim","lento","falha","falhas","falhou","erro","erros","quebrado","perigoso","insatisfeito",
            "piorou","nao funciona","demora","demorou","pessimo","horrivel","decepcionado","fraco",
            "problema","problemas","atraso","atrasada","atrasado","fila","espera","instavel","instabilidade",
            "travou","travando","nao gostei","nao recomendo"
    );

    private static final Set<String> NEGATORS = Set.of("nao","sem","nunca","jamais");
    private static final Pattern P_CONTRASTE =
            Pattern.compile("\\b(mas|porem|porém|contudo|entretanto|no entanto|todavia)\\b", Pattern.CASE_INSENSITIVE);

    @Override
    public Result analyze(String text) {
        String original = text == null ? "" : text.trim();
        String norm = normalize(original);

        // 1) contagem por tokens (com negação) + contagem de frases-chave
        int phrasePos = countPhrases(norm, POS);
        int phraseNeg = countPhrases(norm, NEG);

        String[] split = norm.isBlank() ? new String[0] : norm.split("[^\\p{L}\\p{Nd}]+");
        List<String> tokens = List.of(split);
        int tokenPos = 0, tokenNeg = 0;
        for (int i = 0; i < tokens.size(); i++) {
            String tk = tokens.get(i);
            if (POS.contains(tk)) {
                if (isNegated(tokens, i, 3)) tokenNeg++; else tokenPos++;
            } else if (NEG.contains(tk)) {
                if (isNegated(tokens, i, 3)) tokenPos++; else tokenNeg++;
            }
        }

        // 2) pesos + ajuste por contraste
        double posRaw = phrasePos * 2.0 + tokenPos * 1.0;
        double negRaw = phraseNeg * 2.0 + tokenNeg * 1.0;

        boolean hasContrast = P_CONTRASTE.matcher(norm).find();
        if (hasContrast) {
            double shrink = 0.85; // puxa para MIXED
            posRaw *= shrink; negRaw *= shrink;
        }

        // 3) label e score -1..1
        String label;
        double rawScore;
        if (posRaw > 0 && negRaw > 0) {
            rawScore = (posRaw - negRaw) / (posRaw + negRaw); // -1..1
            label = "MIXED";
        } else if (posRaw > negRaw) {
            rawScore = 0.45 + Math.min(0.55, posRaw * 0.12);
            label = "POSITIVE";
        } else if (negRaw > posRaw) {
            rawScore = -0.45 - Math.min(0.55, negRaw * 0.12);
            label = "NEGATIVE";
        } else {
            rawScore = 0.0;
            label = "NEUTRAL";
        }

        // 4) normaliza para 0..1
        double score01 = clamp((rawScore + 1.0) / 2.0, 0.0, 1.0);
        score01 = round2(score01);

        // 5) resumo e reason simples
        String summary = switch (label) {
            case "POSITIVE" -> "Avaliação positiva.";
            case "NEGATIVE" -> "Avaliação negativa.";
            case "MIXED"    -> "Elogios e críticas no mesmo texto.";
            default         -> "Sem polaridade aparente.";
        };
        String reason = buildReason(posRaw, negRaw, hasContrast, norm);

        // 6) retorna no contrato do IAClient.Result (4 campos)
        return new Result(label, score01, summary, reason);
    }

    /* -------------------- helpers -------------------- */

    private static String normalize(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+","")
                .toLowerCase();
        n = n.replace("ótimo","otimo").replace("ótima","otima")
                .replace("péssimo","pessimo").replace("horrível","horrivel")
                .replace("não","nao");
        n = n.replaceAll("\\s+"," ").trim();
        return n;
    }

    private static int countPhrases(String norm, Set<String> phrases) {
        int hits = 0;
        for (String p : phrases) {
            Pattern pat = Pattern.compile("\\b" + Pattern.quote(p) + "\\b");
            Matcher m = pat.matcher(norm);
            while (m.find()) hits++;
        }
        return hits;
    }

    private static boolean isNegated(List<String> tokens, int idx, int window) {
        int start = Math.max(0, idx - window);
        for (int i = start; i < idx; i++) {
            if (NEGATORS.contains(tokens.get(i))) return true;
        }
        return false;
    }

    private static double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }
    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }

    private static String buildReason(double posRaw, double negRaw, boolean hasContrast, String norm) {
        if (posRaw == 0 && negRaw == 0) return "geral";
        if (hasContrast) {
            // tenta extrair um “assunto” comum em críticas
            if (norm.contains("demora") || norm.contains("fila") || norm.contains("espera") || norm.contains("atraso"))
                return "tempo de espera";
            if (norm.contains("lento") || norm.contains("lentidao")) return "performance";
            if (norm.contains("erro") || norm.contains("falha") || norm.contains("travou")) return "erro";
            return "contraste";
        }
        if (negRaw > posRaw) {
            if (norm.contains("demora") || norm.contains("fila") || norm.contains("espera") || norm.contains("atraso"))
                return "tempo de espera";
            if (norm.contains("lento") || norm.contains("lentidao")) return "performance";
            if (norm.contains("erro") || norm.contains("falha") || norm.contains("travou")) return "erro";
            if (norm.contains("instavel") || norm.contains("instabilidade")) return "instabilidade";
            if (norm.contains("usabilidade") || norm.contains("ux") || norm.contains("ui")) return "usabilidade";
            return "geral";
        }
        return "elogios";
    }
}
