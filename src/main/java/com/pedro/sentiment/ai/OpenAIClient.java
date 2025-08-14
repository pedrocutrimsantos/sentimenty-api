package com.pedro.sentiment.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
public class OpenAIClient implements IAClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAIClient.class);
    private static final URI CHAT_COMPLETIONS_URI = URI.create("https://api.openai.com/v1/chat/completions");

    private final String apiKey;
    private final String model;
    private final double temperature;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenAIClient(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:gpt-4o-mini}") String model,
            @Value("${openai.temperature:0.2}") double temperature
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null ? "gpt-4o-mini" : model.trim();
        this.temperature = temperature;
    }

    @Override
    public Result analyze(String text) {
        if (apiKey.isBlank()) {
            return new Result("NEUTRAL", 0.0, "API key ausente – usando fallback.", "openai.api-key vazio");
        }

        final String system = """
            Você é um analisador de sentimento. Responda ESTRITAMENTE em JSON válido:
            {"sentiment":"POSITIVE|NEGATIVE|NEUTRAL","score":0..1,"summary":"...","reason":"..."}
            """;
        final String user = "Texto:\n---\n" + (text == null ? "" : text) + "\n---\n";

        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", model);
            var messages = mapper.createArrayNode();
            messages.add(msg("system", system));
            messages.add(msg("user", user));
            body.set("messages", messages);
            body.put("temperature", temperature);

            HttpRequest req = HttpRequest.newBuilder(CHAT_COMPLETIONS_URI)
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "sentiment-service/0.1")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() / 100 != 2) {
                log.warn("OpenAI HTTP {}: {}", resp.statusCode(), safe(resp.body()));
                return new Result("NEUTRAL", 0.0, "Falha no provedor – fallback.", "HTTP " + resp.statusCode());
            }

            String content = mapper.readTree(resp.body())
                    .path("choices").path(0).path("message").path("content").asText("");

            if (content.isBlank()) {
                return new Result("NEUTRAL", 0.0, "Resposta vazia – fallback.", "content vazio");
            }

            JsonNode json = mapper.readTree(content);
            String sentiment = normalize(json.path("sentiment").asText("NEUTRAL"));
            double score = clamp(json.path("score").asDouble(0.0), 0.0, 1.0);
            String summary = json.path("summary").asText("Sem resumo.");
            String reason  = json.path("reason").asText("Sem motivo.");

            return new Result(sentiment, score, summary, reason);

        } catch (Exception ex) {
            log.error("Erro OpenAI", ex);
            return new Result("NEUTRAL", 0.0, "Erro inesperado – fallback.", "Ex: " + ex.getClass().getSimpleName());
        }
    }

    private ObjectNode msg(String role, String content) {
        ObjectNode n = mapper.createObjectNode();
        n.put("role", role);
        n.put("content", content);
        return n;
    }
    private static String normalize(String s) {
        if (s == null) return "NEUTRAL";
        return switch (s.trim().toUpperCase()) {
            case "POSITIVE" -> "POSITIVE";
            case "NEGATIVE" -> "NEGATIVE";
            default -> "NEUTRAL";
        };
    }
    private static double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }
    private static String safe(String s) { return s == null ? "" : (s.length() > 500 ? s.substring(0,500)+"..." : s); }
}
