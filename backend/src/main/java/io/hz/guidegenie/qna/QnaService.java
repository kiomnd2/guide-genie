package io.hz.guidegenie.qna;

import io.hz.guidegenie.common.NotFoundException;
import io.hz.guidegenie.embedding.EmbeddingService;
import io.hz.guidegenie.embedding.RetrievedChunk;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 가이드 기반 Q&A. RAG 검색 → LLM 답변을 SSE로 토큰 스트리밍하고, 출처를 함께 반환한다.
 * 근거가 없으면 "가이드에서 답을 찾을 수 없음"으로 응답(환각 방지).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QnaService {

    private static final String NO_ANSWER = "가이드에서 답을 찾을 수 없습니다.";

    private final QnaSessionRepository sessionRepository;
    private final QnaMessageRepository messageRepository;
    private final EmbeddingService embeddingService;

    @Value("${guidegenie.rag.top-k}")
    private int topK;

    // TODO: private final ChatModel chatModel; (스트리밍은 StreamingChatModel)

    @Transactional
    public QnaSession getOrCreateSession(Long projectId, Long sessionId, String userId) {
        if (sessionId != null) {
            return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("QnaSession not found: " + sessionId));
        }
        return sessionRepository.save(new QnaSession(projectId, userId));
    }

    /** SSE로 답변 스트리밍. 컨트롤러에서 생성한 emitter를 받아 비동기로 채운다. */
    @Async
    public void answer(Long projectId, QnaSession session, String question, SseEmitter emitter) {
        try {
            messageRepository.save(new QnaMessage(session.getId(), MessageRole.USER, question, null));

            List<RetrievedChunk> chunks = embeddingService.search(projectId, question, topK);
            if (chunks.isEmpty()) {
                emitter.send(SseEmitter.event().name("token").data(NO_ANSWER));
                persistAnswer(session.getId(), NO_ANSWER, List.of());
                emitter.send(SseEmitter.event().name("done").data("{}"));
                emitter.complete();
                return;
            }

            List<Citation> citations = toCitations(chunks);

            // TODO: chatModel.stream(prompt(question, chunks)) 구독 → 토큰마다 emitter.send(...)
            String answer = "TODO: Gemini 2.5 Flash 스트리밍 답변";
            emitter.send(SseEmitter.event().name("token").data(answer));
            emitter.send(SseEmitter.event().name("citations").data(citations));

            persistAnswer(session.getId(), answer, citations);
            emitter.send(SseEmitter.event().name("done").data("{}"));
            emitter.complete();
        } catch (IOException e) {
            log.warn("[QnA] SSE send failed", e);
            emitter.completeWithError(e);
        }
    }

    @Transactional
    public void feedback(Long messageId, String feedback) {
        QnaMessage message = messageRepository.findById(messageId)
            .orElseThrow(() -> new NotFoundException("QnaMessage not found: " + messageId));
        message.setFeedback(feedback);
    }

    @Transactional
    protected void persistAnswer(Long sessionId, String content, List<Citation> citations) {
        messageRepository.save(
            new QnaMessage(sessionId, MessageRole.ASSISTANT, content, citations));
    }

    private List<Citation> toCitations(List<RetrievedChunk> chunks) {
        return chunks.stream()
            .map(c -> new Citation(
                asLong(c.metadata().get("guide_id")),
                (String) c.metadata().get("title"),
                (String) c.metadata().get("anchor"),
                (String) c.metadata().get("source_url")))
            .toList();
    }

    private Long asLong(Object v) {
        return v instanceof Number n ? n.longValue() : null;
    }
}
