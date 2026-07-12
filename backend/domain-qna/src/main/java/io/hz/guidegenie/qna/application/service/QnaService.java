package io.hz.guidegenie.qna.application.service;

import io.hz.guidegenie.common.NotFoundException;
import io.hz.guidegenie.qna.application.port.out.QnaMessageRepositoryPort;
import io.hz.guidegenie.qna.application.port.out.QnaSessionRepositoryPort;
import io.hz.guidegenie.qna.domain.Citation;
import io.hz.guidegenie.qna.domain.MessageRole;
import io.hz.guidegenie.qna.domain.QnaMessage;
import io.hz.guidegenie.qna.domain.QnaSession;
import io.hz.guidegenie.rag.application.port.in.RetrievedChunk;
import io.hz.guidegenie.rag.application.port.in.SearchPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가이드 기반 Q&A 유스케이스. RAG 검색 → 답변 생성. 근거 없으면 미응답 처리(환각 방지).
 * 스트리밍(SSE)은 인바운드 어댑터가 담당하고, 여기서는 결과를 반환한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QnaService {

    private static final String NO_ANSWER = "가이드에서 답을 찾을 수 없습니다.";

    private final QnaSessionRepositoryPort sessionRepository;
    private final QnaMessageRepositoryPort messageRepository;
    private final SearchPort ragSearch;

    @Value("${guidegenie.rag.top-k}")
    private int topK;

    // TODO: private final ChatModel chatModel; (Spring AI Vertex AI Gemini)

    @Transactional
    public QnaSession getOrCreateSession(Long projectId, Long sessionId, String userId) {
        if (sessionId != null) {
            return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("QnaSession not found: " + sessionId));
        }
        return sessionRepository.save(new QnaSession(projectId, userId));
    }

    @Transactional
    public QnaAnswer answer(Long projectId, QnaSession session, String question) {
        messageRepository.save(new QnaMessage(session.getId(), MessageRole.USER, question, null));

        List<RetrievedChunk> chunks = ragSearch.search(projectId, question, topK);
        if (chunks.isEmpty()) {
            QnaMessage saved = messageRepository.save(
                new QnaMessage(session.getId(), MessageRole.ASSISTANT, NO_ANSWER, List.of()));
            return new QnaAnswer(session.getId(), saved.getId(), NO_ANSWER, List.of());
        }

        List<Citation> citations = toCitations(chunks);
        // TODO: chatModel.call(prompt(question, chunks)) → 답변 생성(스트리밍 시 어댑터에서 토큰 처리)
        String answer = "TODO: Gemini 2.5 Flash 답변";
        QnaMessage saved = messageRepository.save(
            new QnaMessage(session.getId(), MessageRole.ASSISTANT, answer, citations));
        return new QnaAnswer(session.getId(), saved.getId(), answer, citations);
    }

    @Transactional
    public void feedback(Long messageId, String feedback) {
        QnaMessage message = messageRepository.findById(messageId)
            .orElseThrow(() -> new NotFoundException("QnaMessage not found: " + messageId));
        message.setFeedback(feedback);
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
