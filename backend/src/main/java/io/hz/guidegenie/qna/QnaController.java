package io.hz.guidegenie.qna;

import io.hz.guidegenie.common.SecurityUtils;
import io.hz.guidegenie.qna.dto.QnaDtos.AskRequest;
import io.hz.guidegenie.qna.dto.QnaDtos.FeedbackRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class QnaController {

    private static final long SSE_TIMEOUT_MS = 60_000L;

    private final QnaService qnaService;

    /** 질문 → SSE 스트리밍 답변(citations 포함). */
    @PostMapping(value = "/api/projects/{projectId}/qna", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter ask(@PathVariable Long projectId, @Valid @RequestBody AskRequest req) {
        QnaSession session = qnaService.getOrCreateSession(
            projectId, req.sessionId(), SecurityUtils.currentUser());
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        qnaService.answer(projectId, session, req.question(), emitter);
        return emitter;
    }

    /** 답변 피드백(👍/👎). */
    @PostMapping("/api/qna/messages/{messageId}/feedback")
    public void feedback(@PathVariable Long messageId, @Valid @RequestBody FeedbackRequest req) {
        qnaService.feedback(messageId, req.feedback());
    }
}
