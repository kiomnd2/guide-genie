package io.hz.guidegenie.guide.application.service;

import io.hz.guidegenie.guide.domain.Guide;
import io.hz.guidegenie.rag.application.port.in.GenerationPort;
import io.hz.guidegenie.rag.application.port.in.RetrievedChunk;
import io.hz.guidegenie.rag.application.port.in.SearchPort;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * AI 가이드 생성 유스케이스 — RAG 검색(SearchPort) + LLM 생성(GenerationPort) 조합.
 * AI 미설정('ai' 프로파일 아님)이면 stub 초안을 저장한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuideGenerationService {

    private final SearchPort ragSearch;
    private final GenerationPort generation;
    private final GuideService guideService;
    private final GenerationJobTracker jobTracker;

    @Value("${guidegenie.rag.top-k}")
    private int topK;

    /** 단일 프롬프트 생성(비동기). 제목은 프롬프트에서 유추, 미분류로 저장. */
    @Async
    public void generate(Long projectId, String prompt, String author, String jobId) {
        log.info("[AI] generate job={} projectId={}", jobId, projectId);
        create(projectId, deriveTitle(prompt), null, prompt, author);
    }

    /** 항목 단위 생성(비동기). 템플릿 일괄 생성에서 항목마다 호출하며 jobId로 진행 상황을 카운트한다. */
    @Async
    public void generateOne(Long projectId, String title, Long categoryId, String prompt,
                            String author, String jobId) {
        try {
            create(projectId, title, categoryId, prompt, author);
            jobTracker.success(jobId);
        } catch (Exception e) {
            log.warn("[AI] generateOne 실패 title={} : {}", title, e.toString());
            jobTracker.failure(jobId);
        }
    }

    private Guide create(Long projectId, String title, Long categoryId, String prompt, String author) {
        List<RetrievedChunk> context = ragSearch.search(projectId, prompt, topK);

        String markdown;
        if (generation.enabled()) {
            markdown = generation.generate(buildPrompt(title, prompt, context));
        } else {
            markdown = "# " + title + "\n\n> (초안) " + prompt
                + "\n\n> AI 미설정 — 'ai' 프로파일로 실행하면 실제 생성됩니다. docs/AI-SETUP.md";
        }

        Map<String, Object> meta = Map.of(
            "prompt", prompt,
            "model", generation.enabled() ? "gemini-2.5-flash" : "stub",
            "referencedChunks", context.size());

        Guide guide = guideService.createDraft(projectId, title, categoryId, markdown, author, meta);
        log.info("[AI] created draft guideId={} title={} (ai={})", guide.getId(), title, generation.enabled());
        return guide;
    }

    private String buildPrompt(String title, String prompt, List<RetrievedChunk> context) {
        String ctx = context.isEmpty()
            ? "(참조 컨텍스트 없음)"
            : context.stream().map(RetrievedChunk::chunkText).collect(Collectors.joining("\n---\n"));
        return """
            너는 사내 사용 가이드 작성자다. 아래 [컨텍스트]만 근거로 '%s' 가이드를 한국어 Markdown으로 작성해라.
            컨텍스트에 없는 내용은 추측하지 말고 생략한다.

            [요청]
            %s

            [컨텍스트]
            %s
            """.formatted(title, prompt, ctx);
    }

    private String deriveTitle(String prompt) {
        String t = prompt.strip();
        return t.length() > 80 ? t.substring(0, 80) : t;
    }
}
