package com.pedro.sentiment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Feedback de pares (avaliação pessoal).")
public class PeerFeedbackRequest {

    @NotBlank @Schema(example = "pedro")
    private String subjectId;

    @Schema(example = "user-123")
    private String evaluatorId;

    @Schema(example = "Developer")
    private String role;

    @Schema(example = "Portal X")
    private String project;

    @Schema(example = "2025-07")
    private String period;

    @Schema(description="Nota opcional (1..5).")
    private Integer rating;

    @NotBlank
    @Size(min=5, max=3000)
    @Schema(description="Comentário do avaliador.", minLength=5, maxLength=3000, required=true,
            example="O Pedro ajudou muito no design... Contudo, os PRs às vezes demoram...")
    private String text;

    private boolean anonymous;

    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }

    public String getEvaluatorId() { return evaluatorId; }
    public void setEvaluatorId(String evaluatorId) { this.evaluatorId = evaluatorId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getProject() { return project; }
    public void setProject(String project) { this.project = project; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isAnonymous() { return anonymous; }
    public void setAnonymous(boolean anonymous) { this.anonymous = anonymous; }
}
