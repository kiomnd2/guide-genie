package io.hz.guidegenie.ai;

import io.hz.guidegenie.embedding.EmbeddingService;
import io.hz.guidegenie.embedding.RetrievedChunk;
import io.hz.guidegenie.guide.Guide;
import io.hz.guidegenie.guide.GuideService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * RAG 파이프라인 오케스트레이션 (Spring AI + Gemini 2.5 Flash).
 *
 * <ul>
 *   <li>가이드 생성: 프롬프트 → 관련 청크 검색(RAG) → LLM 생성 → Markdown 초안 저장</li>
 *   <li>Q&A: 질문 → 게시 가이드 검색 → 답변 생성(+출처)</li>
 * </ul>
 *
 * 스캐폴드: 검색/저장 흐름은 연결되어 있고, LLM 호출부는 Spring AI {@code ChatModel}로 채운다(TODO).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiOrchestrator {

    private final EmbeddingService embeddingService;
    private final GuideService guideService;

    @Value("${guidegenie.rag.top-k}")
    private int topK;

    // TODO: private final ChatModel chatModel; (Spring AI Vertex AI Gemini)

    /**
     * 프롬프트 기반 가이드 초안 생성(비동기). 완료 시 알림은 별도 채널로.
     */
    @Async
    public void generateGuide(Long projectId, String prompt, String author, String jobId) {
        log.info("[AI] generateGuide job={} projectId={} prompt={}", jobId, projectId, prompt);

        // 1) 관련 소스 청크 검색
        List<RetrievedChunk> context = embeddingService.search(projectId, prompt, topK);

        // 2) LLM으로 Markdown 가이드 생성
        // TODO: chatModel.call(promptTemplate(prompt, context)) → markdown
        String markdown = "# (초안) " + prompt + "\n\n> TODO: Gemini 2.5 Flash로 생성";
        String title = deriveTitle(prompt);

        // 3) 생성 이력과 함께 DRAFT 저장
        Map<String, Object> meta = Map.of(
            "prompt", prompt,
            "model", "gemini-2.5-flash",
            "referencedChunks", context.size()
        );
        Guide guide = guideService.createDraft(projectId, title, markdown, author, meta);
        log.info("[AI] generateGuide job={} → guideId={}", jobId, guide.getId());
    }

    private String deriveTitle(String prompt) {
        String t = prompt.strip();
        return t.length() > 80 ? t.substring(0, 80) : t;
    }
}
