package com.pedro.sentiment.service;

import java.util.Locale;
import java.util.Set;

final class ImprovementSuggester {

    private ImprovementSuggester() {}

    // Áreas que indicam problema (tratadas como negativas)
    private static final Set<String> NEG_AREAS = Set.of(
            "tempo de espera","performance","instabilidade","erro","falha",
            "sistema","usabilidade","comunicacao","documentacao","entrega",
            "disponibilidade","preco","fila","atraso","lentidao"
    );

    static String normalizeReason(String reason) {
        if (reason == null) return "geral";
        String r = reason.toLowerCase(Locale.ROOT).trim();
        if (r.contains("fila") || r.contains("atraso") || r.contains("espera") || r.contains("demora")) return "tempo de espera";
        if (r.contains("erro") || r.contains("bug")) return "erro";
        if (r.contains("falha") || r.contains("travou") || r.contains("queda")) return "falha";
        if (r.contains("instab") || r.contains("estabilidade")) return "instabilidade";
        if (r.contains("lenti") || r.contains("performance") || r.contains("lento")) return "performance";
        if (r.contains("sistema")) return "sistema";
        if (r.contains("usabilidade") || r.contains("ux") || r.contains("ui")) return "usabilidade";
        if (r.contains("comunicacao") || r.contains("comunicação")) return "comunicacao";
        if (r.contains("documentacao") || r.contains("documentação")) return "documentacao";
        if (r.contains("entrega")) return "entrega";
        if (r.contains("disponibilidade")) return "disponibilidade";
        if (r.contains("preco") || r.contains("preço") || r.contains("caro") || r.contains("barato")) return "preco";
        if (r.contains("atendimento") || r.contains("suporte")) return "atendimento";
        return r.isBlank() ? "geral" : r;
    }

    private static boolean hasNegativeCue(String text) {
        if (text == null) return false;
        String t = text.toLowerCase(Locale.ROOT);
        String[] cues = {"mas","porém","porem","no entanto","todavia","contudo","atraso","demora","fila","lento","lentidão","falha","erro","travou","mensagens de erro","não consegui","nao consegui","frustrado","insatisfeito"};
        for (String c : cues) if (t.contains(c)) return true;
        return false;
    }

    static String suggest(String sentiment, String reason, String originalText) {
        String s = sentiment == null ? "" : sentiment.toUpperCase(Locale.ROOT);
        String area = normalizeReason(reason);

        boolean areaNegativa = NEG_AREAS.contains(area);
        boolean textoTemPistaNeg = hasNegativeCue(originalText);

        if (areaNegativa || textoTemPistaNeg || "NEGATIVE".equals(s) || "MIXED".equals(s)) {
            return switch (area) {
                case "tempo de espera" -> "Reduzir tempo de resposta: revisar SLAs, dimensionar equipe nos picos e informar tempo estimado de espera.";
                case "performance"     -> "Otimizar performance: remover gargalos, adicionar caching quando viável e monitorar tempos p95/p99.";
                case "instabilidade"   -> "Elevar estabilidade: corrigir causas-raiz, melhorar observabilidade (logs/métricas/tracing) e usar circuit breakers.";
                case "erro"            -> "Priorizar correção de erros: tratar bugs reincidentes, cobrir com testes e alertar proativamente.";
                case "falha"           -> "Endereçar falhas críticas: reforçar tolerância a falhas, políticas de retry e rollback seguro.";
                case "sistema"         -> "Aumentar confiabilidade do sistema: eliminar pontos de travamento e revisar dependências externas.";
                case "usabilidade"     -> "Melhorar usabilidade: simplificar passos, aprimorar feedback visual e revisar textos/instruções.";
                case "comunicacao"     -> "Aprimorar comunicação: atualizar status proativamente e alinhar expectativas de prazo.";
                case "documentacao"    -> "Atualizar documentação: criar guias curtos, exemplos claros e troubleshooting acessível.";
                case "entrega"         -> "Otimizar fluxo de entrega: alinhar prazos, checagem de qualidade e confirmação de recebimento.";
                case "disponibilidade" -> "Elevar disponibilidade: remover SPOFs, planejar janelas e implementar failover.";
                case "preco"           -> "Reavaliar valor/preço: oferecer planos flexíveis e comunicar benefícios com clareza.";
                case "atendimento"     -> "Padronizar atendimento: roteiros objetivos, treinamento contínuo e acompanhamento de satisfação por contato.";
                default                -> "Aplicar melhoria dirigida: revisar a causa mencionada e definir plano de ação com responsável e prazo.";
            };
        }

        return switch (area) {
            case "atendimento" -> "Manter e documentar boas práticas de atendimento (clareza, cordialidade, resolução rápida).";
            default            -> "Manter o padrão do que funcionou bem; capturar boas práticas e replicar.";
        };
    }
}
