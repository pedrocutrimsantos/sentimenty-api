package com.pedro.sentiment.service;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

public class ReasonExtractor {

    private static final List<String> CANDIDATES = List.of(
            "tempo de espera","fila","atraso","atendimento","suporte","qualidade",
            "performance","lentidao","estabilidade","instabilidade","erro","falha",
            "comunicacao","usabilidade","preco","documentacao","entrega","disponibilidade"
    );

    // sinônimos → rótulo canônico
    private static final Map<String, String> SYN_MAP = Map.ofEntries(
            Map.entry("lento", "lentidao"),
            Map.entry("lentidao", "performance"),
            Map.entry("demora", "tempo de espera"),
            Map.entry("demorado", "tempo de espera"),
            Map.entry("instavel", "instabilidade"),
            Map.entry("queda", "instabilidade"),
            Map.entry("bug", "erro"),
            Map.entry("travou", "falha"),
            Map.entry("travar", "falha"),
            Map.entry("ui", "usabilidade"),
            Map.entry("ux", "usabilidade"),
            Map.entry("preço", "preco"),
            Map.entry("caro", "preco"),
            Map.entry("barato", "preco")
    );

    // conectivos comuns (contraste/causa)
    private static final Pattern P_DEPOIS_DE_MAS      = Pattern.compile("\\b(mas|porém|porem|no entanto|todavia|contudo)\\b(.{0,100})", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_DEPOIS_DE_PORQ     = Pattern.compile("\\b(porque|pois|que)\\b(.{0,100})", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_DEPOIS_DE_PCAUSA   = Pattern.compile("\\b(por causa de|devido a)\\b(.{0,80})", Pattern.CASE_INSENSITIVE);

    public static String extract(String text) {
        if (text == null || text.isBlank()) return "geral";
        String clean = normalize(text);

        // 1) tenta capturar a "cauda" após conectivos (geralmente contém a razão)
        String tail = matchTail(clean);
        if (!tail.isBlank()) {
            String cand = bestCandidateOrSynonym(tail);
            if (!cand.isBlank()) return cand;
        }

        // 2) busca no texto inteiro
        String cand = bestCandidateOrSynonym(clean);
        return cand.isBlank() ? "geral" : cand;
    }

    private static String matchTail(String s) {
        var m1 = P_DEPOIS_DE_MAS.matcher(s);    if (m1.find()) return m1.group(2).trim();
        var m2 = P_DEPOIS_DE_PORQ.matcher(s);   if (m2.find()) return m2.group(2).trim();
        var m3 = P_DEPOIS_DE_PCAUSA.matcher(s); if (m3.find()) return m3.group(2).trim();
        return "";
    }

    private static String bestCandidateOrSynonym(String s) {
        // primeiro tenta mapear sinônimos presentes
        for (var e : SYN_MAP.entrySet()) {
            if (s.contains(e.getKey())) return e.getValue();
        }
        // depois tenta candidatos diretos
        String best = ""; int bestIdx = Integer.MAX_VALUE;
        for (String c : CANDIDATES) {
            int i = s.indexOf(c);
            if (i >= 0 && i < bestIdx) { best = c; bestIdx = i; }
        }
        return best;
    }

    private static String normalize(String s) {
        // remove acentos
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+","");
        n = n.toLowerCase(Locale.ROOT);
        // normalizações abrangentes
        n = n.replace("ótimo","otimo").replace("péssimo","pessimo").replace("não","nao");
        n = n.replace("por\u00E9m", "porem");
        // padroniza whitespace
        n = n.replaceAll("\\s+", " ").trim();
        return n;
    }
}
