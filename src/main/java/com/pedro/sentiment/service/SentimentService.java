package com.pedro.sentiment.service;

import com.pedro.sentiment.ai.IAClient;
import com.pedro.sentiment.dto.FeedbackRequest;
import com.pedro.sentiment.dto.FeedbackResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SentimentService {

    private final IAClient ia;

    @Value("${huggingface.model:unknown}")
    private String model;

    public SentimentService(IAClient ia) {
        this.ia = ia;
    }

    public FeedbackResponse analyze(FeedbackRequest req) {
        IAClient.Result r = ia.analyze(req.getText());

        String iaName = ia.getClass().getSimpleName();
        String provider = iaName.toLowerCase().contains("huggingface")
                ? "HuggingFace: " + (model == null || model.isBlank() ? "unknown" : model)
                : iaName;

        FeedbackResponse out = new FeedbackResponse(r.getSentiment(), r.getScore(), r.getSummary());
        out.setReason(r.getReason());
        out.setProvider(provider);

        String area = ImprovementSuggester.normalizeReason(r.getReason());
        String improvement = ImprovementSuggester.suggest(r.getSentiment(), r.getReason(), req.getText());
        out.setImprovementArea(area);
        out.setImprovement(improvement);

        return out;
    }
}
