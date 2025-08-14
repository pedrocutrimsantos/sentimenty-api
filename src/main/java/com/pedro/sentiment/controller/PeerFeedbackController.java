package com.pedro.sentiment.controller;

import com.pedro.sentiment.dto.PeerFeedbackRequest;
import com.pedro.sentiment.dto.PeerFeedbackResponse;
import com.pedro.sentiment.service.PeerFeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/v1/peer-feedback")
public class PeerFeedbackController {
    private final PeerFeedbackService service;
    public PeerFeedbackController(PeerFeedbackService service) { this.service = service; }

    @PostMapping(value="/analyze", consumes="application/json", produces="application/json")
    public PeerFeedbackResponse analyze(@Valid @RequestBody PeerFeedbackRequest req) {
        return service.analyze(req);
    }
}