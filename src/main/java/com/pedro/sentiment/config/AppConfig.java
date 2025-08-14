package com.pedro.sentiment.config;

import com.pedro.sentiment.ai.IAClient;
import com.pedro.sentiment.ai.MockClient;
import com.pedro.sentiment.ai.OpenAIClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Value("${app.ai.provider:mock}")
    private String provider;

//    @Bean
    public IAClient iaClient(OpenAIClient openAIClient) {
        return switch (provider.toLowerCase()) {
            case "openai" -> openAIClient;
            default -> new MockClient();
        };
    }
}
