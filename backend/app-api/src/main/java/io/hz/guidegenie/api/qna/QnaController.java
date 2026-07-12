package io.hz.guidegenie.api.qna;

import io.hz.guidegenie.common.SecurityUtils;
import io.hz.guidegenie.qna.application.service.QnaAnswer;
import io.hz.guidegenie.qna.application.service.QnaService;
import io.hz.guidegenie.qna.domain.QnaSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Q&A 인바운드 어댑터. 도메인 서비스는 결과(QnaAnswer)만 반환하고, 여기서 SSE로 스트리밍한다
 * (웹 기술을 도메인 밖으로 유지).
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class QnaController {

    private static final long SSE_TIMEOUT_MS = 60_000L;

    private final QnaService qnaService;

    @PostMapping(value = "/api/projects/{projectId}/qna", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter ask(@PathVariable Long projectId, @Valid @RequestBody AskRequest req) {
        QnaSession session = qnaService.getOrCreateSession(
            projectId, req.sessionId(), SecurityUtils.currentUser());
        QnaAnswer answer = qnaService.answer(projectId, session, req.question());

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        try {
            emitter.send(SseEmitter.event().name("token").data(answer.answer()));
            emitter.send(SseEmitter.event().name("citations").data(answer.citations()));
            emitter.send(SseEmitter.event().name("done")
                .data("{\"sessionId\":" + answer.sessionId() + ",\"messageId\":" + answer.messageId() + "}"));
            emitter.complete();
        } catch (IOException e) {
            log.warn("[QnA] SSE send failed", e);
            emitter.completeWithError(e);
        }
        return emitter;
    }

    @PostMapping("/api/qna/messages/{messageId}/feedback")
    public void feedback(@PathVariable Long messageId, @Valid @RequestBody FeedbackRequest req) {
        qnaService.feedback(messageId, req.feedback());
    }

    public record AskRequest(Long sessionId, @NotBlank String question) {}

    public record FeedbackRequest(@Pattern(regexp = "UP|DOWN") String feedback) {}
}
