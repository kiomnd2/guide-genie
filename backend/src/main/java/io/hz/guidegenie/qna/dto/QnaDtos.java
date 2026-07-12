package io.hz.guidegenie.qna.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class QnaDtos {

    private QnaDtos() {
    }

    public record AskRequest(
        Long sessionId,
        @NotBlank String question
    ) {
    }

    public record FeedbackRequest(
        @Pattern(regexp = "UP|DOWN") String feedback
    ) {
    }
}
