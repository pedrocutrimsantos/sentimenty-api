package com.pedro.sentiment.peer;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class PeerAspectExtractor {

    private static final Map<String,String> SYN = Map.ofEntries(
            Map.entry("arquitetura","arquitetura"),
            Map.entry("design","arquitetura"),
            Map.entry("projeto de componentes","arquitetura"),
            Map.entry("qualidade de codigo","qualidade"),
            Map.entry("qualidade","qualidade"),
            Map.entry("teste","qualidade"),
            Map.entry("testes","qualidade"),
            Map.entry("documentacao","documentação"),
            Map.entry("docs","documentação"),
            Map.entry("comunicacao","comunicação"),
            Map.entry("alinhamento","comunicação"),
            Map.entry("colaboracao","colaboração"),
            Map.entry("pareamento","colaboração"),
            Map.entry("mentoria","mentoria"),
            Map.entry("lideranca","liderança"),
            Map.entry("ownership","ownership"),
            Map.entry("proatividade","proatividade"),
            Map.entry("debug","debugging"),
            Map.entry("investigacao","debugging"),
            Map.entry("performance","performance"),
            Map.entry("lento","performance"),
            Map.entry("velocidade","velocidade/tempo de resposta"),
            Map.entry("tempo de resposta","velocidade/tempo de resposta"),
            Map.entry("review","velocidade/tempo de resposta"),
            Map.entry("pr","velocidade/tempo de resposta"),
            Map.entry("prazo","entrega"),
            Map.entry("entrega","entrega"),
            Map.entry("requisitos","entendimento de requisitos"),
            Map.entry("negocio","entendimento de requisitos")
    );

    private static final Pattern WORD = Pattern.compile("\\p{L}+");

    public static String canonicalAspect(String text) {
        if (text == null || text.isBlank()) return "geral";
        String norm = normalize(text);
        var m = WORD.matcher(norm);
        while (m.find()) {
            String token = m.group();
            for (var e : SYN.entrySet()) {
                if (token.equals(e.getKey()) || norm.contains(e.getKey())) {
                    return e.getValue();
                }
            }
        }
        return "geral";
    }

    private static String normalize(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+","");
        n = n.toLowerCase(Locale.ROOT);
        n = n.replaceAll("\\s+"," ").trim();
        return n;
    }
}
