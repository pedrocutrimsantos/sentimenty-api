package com.pedro.sentiment.service;

import com.pedro.sentiment.ai.IAClient;
import com.pedro.sentiment.ai.SupportsDetailed;
import com.pedro.sentiment.dto.PeerFeedbackRequest;
import com.pedro.sentiment.dto.PeerFeedbackResponse;
import com.pedro.sentiment.peer.PeerAspectExtractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.BreakIterator;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class PeerFeedbackService {

    private static final double POS_STRENGTH_MIN = 0.75;
    private static final double NEG_IMPROVE_MIN  = 0.35;
    private static final int    MAX_STRENGTHS    = 3;
    private static final int    MAX_IMPROVEMENTS = 3;
    private static final int    MAX_SENTENCES    = 12;

    private final IAClient ia;

    @Value("${huggingface.model:unknown}")
    private String model;

    public PeerFeedbackService(IAClient ia) { this.ia = ia; }

    public PeerFeedbackResponse analyze(PeerFeedbackRequest req) {
        var now = OffsetDateTime.now();

        IAClient.Result overall;
        List<SupportsDetailed.SentenceSentiment> per = List.of();

        if (ia instanceof SupportsDetailed sd) {
            var det = sd.analyzeDetailed(req.getText());
            overall = det.overall();
            per = det.perSentence();
        } else {
            List<String> sentences = split(req.getText(), MAX_SENTENCES);
            List<SupportsDetailed.SentenceSentiment> tmp = new ArrayList<>();
            for (String s : sentences) {
                var r = ia.analyze(s);
                tmp.add(new SupportsDetailed.SentenceSentiment(s, r.getSentiment(), r.getScore()));
            }
            per = tmp;
            overall = ia.analyze(req.getText());
        }

        Map<String,double[]> acc = new LinkedHashMap<>(); // aspecto -> [pos,neg]
        List<PeerFeedbackResponse.Strength> strengths = new ArrayList<>();

        // NOVO: guardar melhor evidência negativa por aspecto
        Map<String, SupportsDetailed.SentenceSentiment> bestNegEvidence = new HashMap<>();

        for (var s : per) {
            String aspect = PeerAspectExtractor.canonicalAspect(s.sentence());
            acc.putIfAbsent(aspect, new double[]{0,0});

            switch (s.label()) {
                case "POSITIVE" -> {
                    acc.get(aspect)[0] += s.score();
                    if (s.score() >= POS_STRENGTH_MIN && strengths.size() < MAX_STRENGTHS) {
                        strengths.add(new PeerFeedbackResponse.Strength(aspect, s.sentence()));
                    }
                }
                case "NEGATIVE" -> {
                    acc.get(aspect)[1] += s.score();
                    // Atualiza evidência negativa mais forte
                    var cur = bestNegEvidence.get(aspect);
                    if (cur == null || s.score() > cur.score()) {
                        bestNegEvidence.put(aspect, s);
                    }
                }
            }
        }

        // SÓ usa "geral" se nada foi classificado em aspecto
        if (acc.isEmpty()) {
            acc.put("geral", new double[]{ Math.max(0, overall.getScore()), 0 });
        } else {
            acc.remove("geral"); // <-- garante que não duplica se já temos aspectos
        }

        // Ordena por negatividade
        var orderedNeg = acc.entrySet().stream()
                .sorted((a,b) -> Double.compare(b.getValue()[1], a.getValue()[1]))
                .toList();

        List<PeerFeedbackResponse.Improvement> improvements = new ArrayList<>();
        for (var e : orderedNeg) {
            var aspect = e.getKey();
            double neg = e.getValue()[1];
            if (neg >= NEG_IMPROVE_MIN && improvements.size() < MAX_IMPROVEMENTS) {
                var suggestion = ImprovementSuggester.suggest("NEGATIVE", aspect, req.getText());
                var ev = bestNegEvidence.get(aspect);
                String evidence = ev != null ? ev.sentence() : null; // usa a pior frase negativa do aspecto
                improvements.add(new PeerFeedbackResponse.Improvement(aspect, suggestion, evidence));
            }
        }

        var resp = new PeerFeedbackResponse();
        resp.subjectId   = req.getSubjectId();
        resp.sentiment   = overall.getSentiment();
        resp.score       = overall.getScore();
        resp.summary     = overall.getSummary();
        resp.strengths   = strengths;
        resp.improvements= improvements;

        var aspectScores = new ArrayList<PeerFeedbackResponse.AspectScore>();
        for (var e : acc.entrySet()) {
            aspectScores.add(new PeerFeedbackResponse.AspectScore(
                    e.getKey(), round(e.getValue()[0]), round(e.getValue()[1])
            ));
        }
        resp.aspects = aspectScores;

        String iaName = ia.getClass().getSimpleName();
        resp.provider = iaName.toLowerCase().contains("huggingface") ? "HuggingFace: " + model : iaName;
        resp.timestamp = now.toString();
        return resp;
    }

    private static double round(double v) { return Math.round(v * 100.0) / 100.0; }

    private static List<String> split(String text, int max) {
        if (text == null || text.isBlank()) return List.of();
        List<String> out = new ArrayList<>();
        BreakIterator bi = BreakIterator.getSentenceInstance(new Locale("pt","BR"));
        bi.setText(text);
        int start = bi.first();
        for (int end = bi.next(); end != BreakIterator.DONE; start = end, end = bi.next()) {
            String s = text.substring(start, end).trim();
            if (!s.isEmpty()) out.add(s);
            if (out.size() >= max) break;
        }
        if (out.isEmpty()) out.add(text.trim());
        return out;
    }
}
