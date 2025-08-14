package com.pedro.sentiment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Representa uma requisição para análise de sentimento de um texto.")
public class FeedbackRequest {

    @Schema(
            description = "Texto a ser analisado pelo serviço de IA.",
            example = "O atendimento foi excelente, mas o tempo de espera poderia melhorar.",
            minLength = 5,
            maxLength = 2000,
            required = true
    )
    @NotBlank(message = "O campo 'text' não pode estar vazio.")
    @Size(min = 5, max = 2000, message = "O texto deve ter entre 5 e 2000 caracteres.")
    private String text;

    @Schema(
            description = "Origem do feedback (ex.: nome do setor, equipe, canal de atendimento).",
            example = "Suporte Técnico"
    )
    private String source;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
