package com.pedro.sentiment.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.BreakIterator;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "huggingface")
public class HuggingFaceClient implements IAClient, SupportsDetailed {

    private static final Logger log = LoggerFactory.getLogger(HuggingFaceClient.class);

    private final String token;
    private final String model;
    private final int timeoutSeconds;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    // Limiar para MIXED (pode tornar configurável via @Value)
    private static final double POS_STRONG = 0.55;
    private static final double NEG_STRONG = 0.55;

    public HuggingFaceClient(
            @Value("${huggingface.token:}") String token,
            @Value("${huggingface.model:cardiffnlp/twitter-xlm-roberta-base-sentiment}") String model,
            @Value("${huggingface.timeout-seconds:60}") int timeoutSeconds
    ) {
        String tk = safeTrim(token);
        if (isBlank(tk)) tk = safeTrim(System.getenv("HUGGINGFACE_TOKEN"));
        if (isBlank(tk)) tk = safeTrim(System.getProperty("huggingface.token"));

        this.token = tk == null ? "" : tk;
        this.model = (model == null || model.isBlank())
                ? "cardiffnlp/twitter-xlm-roberta-base-sentiment" : model.trim();
        this.timeoutSeconds = timeoutSeconds;

        log.info("HuggingFaceClient: provider=hf, model={}, tokenPresente={}",
                this.model, this.token.isBlank() ? "NÃO" : "SIM(len=" + this.token.length() + ")");
    }

    // ---------- API simples (overall) ----------
    @Override
    public Result analyze(String text) {
        if (token.isBlank()) {
            return new Result("NEUTRAL", 0.0, "Token HF ausente – fallback.", "geral");
        }
        try {
            List<String> sentences = splitSentences(text == null ? "" : text.trim());
            if (sentences.isEmpty()) sentences = List.of("");

            // Sempre {"inputs": ...}
            String payload = mapper.writeValueAsString(
                    sentences.size() == 1
                            ? new PayloadSingle(sentences.get(0))
                            : new PayloadBatch(sentences)
            );

            String url = "https://api-inference.huggingface.co/models/" + model;
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "sentiment-service/0.3")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            int attempts = 0;
            HttpResponse<String> resp;
            while (true) {
                attempts++;
                resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int sc = resp.statusCode();
                if (sc / 100 == 2) break;
                if (attempts >= 4 || !(sc == 429 || sc == 503)) break;
                Thread.sleep(350L * attempts);
            }

            if (resp.statusCode() / 100 != 2) {
                log.warn("HF HTTP {}: {}", resp.statusCode(), safe(resp.body()));
                return new Result("NEUTRAL", 0.0, "Falha HF – fallback.", "geral");
            }

            JsonNode root = mapper.readTree(resp.body());

            // single: [ {label,score}... ]
            // batch : [ [ {label,score}... ], [ ... ] , ... ]
            List<LabelScore> bestPerSentence = new ArrayList<>();
            if (root.isArray() && root.size() > 0 && root.get(0).isArray()) {
                for (JsonNode sentenceScores : root) bestPerSentence.add(extractBest(sentenceScores));
            } else if (root.isArray()) {
                bestPerSentence.add(extractBest(root));
            } else {
                return new Result("NEUTRAL", 0.0, "Resposta inesperada – fallback.", "geral");
            }

            double posSum = 0, negSum = 0, neuSum = 0;
            int n = bestPerSentence.size();
            for (LabelScore ls : bestPerSentence) {
                switch (ls.label) {
                    case "POSITIVE" -> posSum += ls.score;
                    case "NEGATIVE" -> negSum += ls.score;
                    case "NEUTRAL"  -> neuSum += ls.score;
                }
            }
            double posAvg = posSum / n;
            double negAvg = negSum / n;
            double neuAvg = neuSum / n;

            String overall;
            double overallScore;
            String summary;

            if (posAvg >= POS_STRONG && negAvg >= NEG_STRONG) {
                overall = "MIXED";
                overallScore = Math.max(posAvg, negAvg);
                summary = "Sinais positivos e negativos relevantes em diferentes partes do texto.";
            } else if (posAvg >= negAvg && posAvg >= neuAvg) {
                overall = "POSITIVE";
                overallScore = posAvg;
                summary = "Avaliação positiva predominante.";
            } else if (negAvg >= posAvg && negAvg >= neuAvg) {
                overall = "NEGATIVE";
                overallScore = negAvg;
                summary = "Avaliação negativa predominante.";
            } else {
                overall = "NEUTRAL";
                overallScore = neuAvg;
                summary = "Sem polaridade clara; avaliação neutra.";
            }

            // Motivo heurístico
            String reason = inferReason(text);

            return new IAClient.Result(
                    overall,
                    overallScore, // aqui estava 'score', mas a variável correta é 'overallScore'
                    summary,
                    reason,
                    List.of(new IAClient.AspectScore("geral", posAvg, negAvg)),
                    "HuggingFace: " + model
            );

        } catch (Exception ex) {
            log.error("Erro HF", ex);
            return new Result("NEUTRAL", 0.0, "Erro inesperado – fallback.", "geral");
        }
    }

    // ---------- API detalhada (por sentença) ----------
    @Override
    public SupportsDetailed.DetailedResult analyzeDetailed(String text) {
        if (token.isBlank()) {
            var overall = new Result("NEUTRAL", 0.0, "Token HF ausente – fallback.", "geral");
            return new SupportsDetailed.DetailedResult(overall, List.of());
        }
        try {
            List<String> sentences = splitSentences(text == null ? "" : text.trim());
            if (sentences.isEmpty()) sentences = List.of("");

            String payload = mapper.writeValueAsString(new PayloadBatch(sentences));
            String url = "https://api-inference.huggingface.co/models/" + model;

            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "sentiment-service/0.3")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            int attempts = 0;
            HttpResponse<String> resp;
            while (true) {
                attempts++;
                resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int sc = resp.statusCode();
                if (sc / 100 == 2) break;
                if (attempts >= 4 || !(sc == 429 || sc == 503)) break;
                Thread.sleep(350L * attempts);
            }

            if (resp.statusCode() / 100 != 2) {
                log.warn("HF HTTP {}: {}", resp.statusCode(), safe(resp.body()));
                IAClient.Result overall = analyze(text);
                return new SupportsDetailed.DetailedResult(overall, List.of());
            }

            JsonNode root = mapper.readTree(resp.body());
            List<LabelScore> bestPerSentence = new ArrayList<>();
            if (root.isArray() && root.size() > 0 && root.get(0).isArray()) {
                for (JsonNode sentenceScores : root) bestPerSentence.add(extractBest(sentenceScores));
            } else if (root.isArray()) {
                bestPerSentence.add(extractBest(root));
            } else {
                IAClient.Result overall = analyze(text);
                return new SupportsDetailed.DetailedResult(overall, List.of());
            }

            double pos=0, neg=0, neu=0;
            for (LabelScore ls : bestPerSentence) {
                switch (ls.label) {
                    case "POSITIVE" -> pos += ls.score;
                    case "NEGATIVE" -> neg += ls.score;
                    case "NEUTRAL"  -> neu += ls.score;
                }
            }
            int n = Math.max(1, bestPerSentence.size());
            double posAvg = pos/n, negAvg = neg/n, neuAvg = neu/n;

            String overallLbl;
            double overallScore;
            if (posAvg >= POS_STRONG && negAvg >= NEG_STRONG) { overallLbl = "MIXED"; overallScore = Math.max(posAvg, negAvg); }
            else if (posAvg >= negAvg && posAvg >= neuAvg)     { overallLbl = "POSITIVE"; overallScore = posAvg; }
            else if (negAvg >= posAvg && negAvg >= neuAvg)     { overallLbl = "NEGATIVE"; overallScore = negAvg; }
            else                                               { overallLbl = "NEUTRAL";  overallScore = neuAvg; }

            String summary = switch (overallLbl) {
                case "POSITIVE" -> "Avaliação positiva predominante.";
                case "NEGATIVE" -> "Avaliação negativa predominante.";
                case "MIXED"    -> "Sinais positivos e negativos relevantes em diferentes partes do texto.";
                default         -> "Sem polaridade clara; avaliação neutra.";
            };

            String reason = inferReason(text);
            IAClient.Result overall = new IAClient.Result(overallLbl, clamp(overallScore,0,1), summary, reason);

            List<SupportsDetailed.SentenceSentiment> per = new ArrayList<>();
            for (int i = 0; i < bestPerSentence.size(); i++) {
                var ls = bestPerSentence.get(i);
                var s  = i < sentences.size() ? sentences.get(i) : "";
                per.add(new SupportsDetailed.SentenceSentiment(s, ls.label, ls.score));
            }

            return new SupportsDetailed.DetailedResult(overall, per);

        } catch (Exception ex) {
            log.error("Erro HF (detailed)", ex);
            IAClient.Result overall = analyze(text);
            return new SupportsDetailed.DetailedResult(overall, List.of());
        }
    }

    // ===== helpers =====

    private static LabelScore extractBest(JsonNode arr) {
        if (arr == null || !arr.isArray() || arr.size() == 0) return new LabelScore("NEUTRAL", 0.0);
        String bestLabel = "NEUTRAL";
        double bestScore = 0.0;
        for (JsonNode n : arr) {
            String lbl = n.path("label").asText("").toUpperCase(Locale.ROOT).trim();
            double sc = n.path("score").asDouble(0.0);
            if (!lbl.isBlank() && sc > bestScore) {
                bestScore = sc;
                bestLabel = normalize(lbl);
            }
        }
        return new LabelScore(bestLabel, bestScore);
    }

    private static List<String> splitSentences(String text) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank()) return out;
        BreakIterator bi = BreakIterator.getSentenceInstance(new Locale("pt", "BR"));
        bi.setText(text);
        int start = bi.first();
        for (int end = bi.next(); end != BreakIterator.DONE; start = end, end = bi.next()) {
            String s = text.substring(start, end).trim();
            if (!s.isEmpty()) out.add(s);
        }
        if (out.isEmpty()) out.add(text);
        return out;
    }

    private static String normalize(String label) {
        if (label == null) return "NEUTRAL";
        String up = label.toUpperCase(Locale.ROOT);
        return switch (up) {
            case "POSITIVE", "LABEL_2", "5 STARS", "4 STARS" -> "POSITIVE";
            case "NEGATIVE", "LABEL_0", "1 STAR", "2 STARS" -> "NEGATIVE";
            case "NEUTRAL", "LABEL_1", "3 STARS" -> "NEUTRAL";
            default -> "NEUTRAL";
        };
    }

    // Heurística leve para "reason"
    private static String inferReason(String text) {
        if (text == null || text.isBlank()) return "geral";
        String s = normalizeText(text);
        String tail = matchTail(s);
        String picked = bestCandidateOrSynonym(!tail.isBlank() ? tail : s);
        return picked.isBlank() ? "geral" : picked;
    }

    private static String normalizeText(String s) {
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+","");
        n = n.toLowerCase(Locale.ROOT);
        n = n.replaceAll("\\s+", " ").trim();
        n = n.replace("por\u00E9m", "porem");
        return n;
    }

    private static String matchTail(String s) {
        String[] regs = {
                "\\b(mas|porem|porém|no entanto|todavia|contudo)\\b(.{0,100})",
                "\\b(porque|pois|que)\\b(.{0,100})",
                "\\b(por causa de|devido a)\\b(.{0,80})"
        };
        for (String r : regs) {
            var m = java.util.regex.Pattern.compile(r, java.util.regex.Pattern.CASE_INSENSITIVE).matcher(s);
            if (m.find()) return m.group(2).trim();
        }
        return "";
    }

    private static String bestCandidateOrSynonym(String s) {
        String[][] syn = {
                {"lento","lentidao"},{"demora","tempo de espera"},{"demorado","tempo de espera"},
                {"instavel","instabilidade"},{"queda","instabilidade"},{"bug","erro"},
                {"travou","falha"},{"travar","falha"},{"ui","usabilidade"},{"ux","usabilidade"},
                {"preço","preco"},{"caro","preco"},{"barato","preco"}
        };
        for (String[] kv : syn) if (s.contains(kv[0])) return kv[1];

        String[] candidates = {
                "tempo de espera","fila","atraso","atendimento","suporte","qualidade",
                "performance","lentidao","estabilidade","instabilidade","erro","falha",
                "comunicacao","usabilidade","preco","documentacao","entrega","disponibilidade"
        };
        String best=""; int idx=Integer.MAX_VALUE;
        for (String c : candidates) {
            int i = s.indexOf(c);
            if (i>=0 && i<idx) { best=c; idx=i; }
        }
        return best;
    }

    private static double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }
    private static String safe(String s) { return s == null ? "" : (s.length() > 500 ? s.substring(0, 500) + "..." : s); }
    private static String safeTrim(String s) { return s == null ? null : s.trim(); }
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private record PayloadSingle(String inputs) {}
    private record PayloadBatch(List<String> inputs) {}
    private record LabelScore(String label, double score) {}
}
