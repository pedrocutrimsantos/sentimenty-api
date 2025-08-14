package com.pedro.sentiment.ai;

import java.util.List;

public interface SupportsDetailed {
    record SentenceSentiment(String sentence, String label, double score) {}
    record DetailedResult(IAClient.Result overall, List<SentenceSentiment> perSentence) {}
    DetailedResult analyzeDetailed(String text);
}