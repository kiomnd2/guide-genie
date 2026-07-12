package io.hz.guidegenie.guide.application.service;

import io.hz.guidegenie.guide.domain.Guide;
import io.hz.guidegenie.rag.application.port.in.RetrievedChunk;
import io.hz.guidegenie.rag.application.port.in.SearchPort;
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

    /** 단일 프롬프트 생성(비동기). 제목은 프롬프트에서 유추, 미분류로 저장. */
    @Async
    public void generate(Long projectId, String prompt, String author, String jobId) {
        log.info("[AI] generate job={} projectId={}", jobId, projectId);
        create(projectId, deriveTitle(prompt), null, prompt, author);
    }

    /** 항목 단위 생성(비동기). 템플릿 일괄 생성에서 항목마다 호출. */
    @Async
    public void generateOne(Long projectId, String title, Long categoryId, String prompt, String author) {
        create(projectId, title, categoryId, prompt, author);
    }

    private Guide create(Long projectId, String title, Long categoryId, String prompt, String author) {
        List<RetrievedChunk> context = ragSearch.search(projectId, prompt, topK);

        // TODO: chatModel.call(promptTemplate(prompt, context)) → markdown
        String markdown = "# " + title + "\n\n> (초안) " + prompt + "\n\n> TODO: Gemini 2.5 Flash로 생성";
        Map<String, Object> meta = Map.of(
            "prompt", prompt,
            "model", "gemini-2.5-flash",
            "referencedChunks", context.size());

        Guide guide = guideService.createDraft(projectId, title, categoryId, markdown, author, meta);
        log.info("[AI] created draft guideId={} title={}", guide.getId(), title);
        return guide;
    }

    private String deriveTitle(String prompt) {
        String t = prompt.strip();
        return t.length() > 80 ? t.substring(0, 80) : t;
    }
}
