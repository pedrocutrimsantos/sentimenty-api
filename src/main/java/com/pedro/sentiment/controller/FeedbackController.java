package com.pedro.sentiment.controller;

import com.pedro.sentiment.dto.FeedbackRequest;
import com.pedro.sentiment.dto.FeedbackResponse;
import com.pedro.sentiment.service.SentimentService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/v1")
public class FeedbackController {

    private final SentimentService service;

    public FeedbackController(SentimentService service) { this.service = service; }

    @PostMapping(
            value = "/sentiment",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<FeedbackResponse> analyze(@Valid @RequestBody FeedbackRequest req){
        return ResponseEntity.ok(service.analyze(req));
    }
}
