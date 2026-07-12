package io.hz.guidegenie.guide.application.service;

import io.hz.guidegenie.rag.application.port.in.RetrievedChunk;
import io.hz.guidegenie.rag.application.port.in.SearchPort;
import io.hz.guidegenie.guide.domain.Guide;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * AI 가이드 생성 유스케이스 — RAG 검색(SearchPort) + 가이드 생성(GuideService) 조합.
 *
 * <p>스캐폴드: 검색/저장 흐름은 연결. LLM 호출부는 Spring AI ChatModel(Gemini)로 채운다(TODO).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuideGenerationService {

    private final SearchPort ragSearch; // domain-rag 인바운드 포트
    private final GuideService guideService;

    @Value("${guidegenie.rag.top-k}")
    private int topK;

    // TODO: private final ChatModel chatModel; (Spring AI Vertex AI Gemini)

    /** 프롬프트 기반 가이드 초안 생성(비동기). 완료 시 미분류 DRAFT로 저장. */
    @Async
    public void generate(Long projectId, String prompt, String author, String jobId) {
        log.info("[AI] generate job={} projectId={} prompt={}", jobId, projectId, prompt);

        List<RetrievedChunk> context = ragSearch.search(projectId, prompt, topK);

        // TODO: chatModel.call(promptTemplate(prompt, context)) → markdown
        String markdown = "# (초안) " + prompt + "\n\n> TODO: Gemini 2.5 Flash로 생성";
        String title = deriveTitle(prompt);
        Map<String, Object> meta = Map.of(
            "prompt", prompt,
            "model", "gemini-2.5-flash",
            "referencedChunks", context.size());

        Guide guide = guideService.createDraft(projectId, title, null, markdown, author, meta);
        log.info("[AI] generate job={} → guideId={}", jobId, guide.getId());
    }

    private String deriveTitle(String prompt) {
        String t = prompt.strip();
        return t.length() > 80 ? t.substring(0, 80) : t;
    }
}
