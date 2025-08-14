package com.pedro.sentiment;

import com.pedro.sentiment.ai.MockClient;
import com.pedro.sentiment.dto.FeedbackRequest;
import com.pedro.sentiment.dto.FeedbackResponse;
import com.pedro.sentiment.service.SentimentService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SentimentServiceTest {

    @Test
    void mockPositive(){
        var service = new SentimentService(new MockClient());
        var req = new com.pedro.sentiment.dto.FeedbackRequest();
        req.setText("O serviço foi ótimo e eficiente!");
        FeedbackResponse res = service.analyze(req);
        assertEquals("POSITIVE", res.getSentiment());
        assertTrue(res.getScore() >= 0.5);
    }
}
