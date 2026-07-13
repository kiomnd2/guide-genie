package io.hz.guidegenie.guide.application.service;

import io.hz.guidegenie.guide.domain.Guide;
import io.hz.guidegenie.guide.domain.TemplateItem;
import io.hz.guidegenie.rag.application.port.in.GenerationPort;
import io.hz.guidegenie.rag.application.port.in.RetrievedChunk;
import io.hz.guidegenie.rag.application.port.in.SearchPort;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * AI 가이드 생성 유스케이스 — RAG 검색(SearchPort) + LLM 생성(GenerationPort) 조합.
 * 템플릿 항목({@link TemplateItem})의 대상 독자·목차·상세 수준을 프롬프트에 반영해 상세한 가이드를 생성한다.
 * AI 미설정('ai' 프로파일 아님)이면 목차 골격을 갖춘 stub 초안을 저장한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuideGenerationService {

    private final SearchPort ragSearch;
    private final GenerationPort generation;
    private final GuideService guideService;
    private final GenerationJobTracker jobTracker;

    /** 가이드 생성 시 검색할 컨텍스트 청크 수. RUNBOOK 수준 근거 확보를 위해 QnA(top-k)보다 크게 잡는다. */
    @Value("${guidegenie.rag.generate-top-k:12}")
    private int generateTopK;

    /** 단일 프롬프트 생성(비동기). 제목은 프롬프트에서 유추, 미분류로 저장. */
    @Async
    public void generate(Long projectId, String prompt, String author, String jobId) {
        log.info("[AI] generate job={} projectId={}", jobId, projectId);
        create(projectId, new TemplateItem(deriveTitle(prompt), prompt, null), author);
    }

    /** 항목 단위 생성(비동기). 템플릿 일괄 생성에서 항목마다 호출하며 jobId로 진행 상황을 카운트한다. */
    @Async
    public void generateOne(Long projectId, TemplateItem item, String author, String jobId) {
        try {
            create(projectId, item, author);
            jobTracker.success(jobId);
        } catch (Exception e) {
            log.warn("[AI] generateOne 실패 title={} : {}", item.title(), e.toString());
            jobTracker.failure(jobId);
        }
    }

    private Guide create(Long projectId, TemplateItem item, String author) {
        List<RetrievedChunk> context = ragSearch.search(projectId, retrievalQuery(item), generateTopK);

        String markdown = generation.enabled()
            ? generation.generate(buildPrompt(item, context))
            : stubMarkdown(item);

        Map<String, Object> meta = Map.of(
            "prompt", item.prompt(),
            "audience", item.audience() == null ? "" : item.audience(),
            "sections", item.sections(),
            "detailLevel", item.detailLevel().name(),
            "model", generation.enabled() ? "gemini-2.5-flash" : "stub",
            "referencedChunks", context.size());

        Guide guide = guideService.createDraft(projectId, item.title(), item.categoryId(), markdown, author, meta);
        log.info("[AI] created draft guideId={} title={} sections={} level={} (ai={})",
            guide.getId(), item.title(), item.sections().size(), item.detailLevel(), generation.enabled());
        return guide;
    }

    /** 검색 재현율을 높이기 위해 제목·요청·목차를 합쳐 질의로 사용한다. */
    private String retrievalQuery(TemplateItem item) {
        StringBuilder q = new StringBuilder(item.title()).append('\n').append(item.prompt());
        if (!item.sections().isEmpty()) {
            q.append('\n').append(String.join(", ", item.sections()));
        }
        return q.toString();
    }

    private String buildPrompt(TemplateItem item, List<RetrievedChunk> context) {
        String ctx = context.isEmpty()
            ? "(참조 컨텍스트 없음)"
            : IntStream.range(0, context.size())
                .mapToObj(i -> "[출처 " + (i + 1) + "] " + context.get(i).chunkText())
                .collect(Collectors.joining("\n\n---\n\n"));

        String audience = (item.audience() == null || item.audience().isBlank())
            ? "해당 시스템을 인수인계받는 개발자·운영자" : item.audience().trim();

        String outline = item.sections().isEmpty()
            ? "  - (목차 지정 없음) 주제에 맞는 논리적 목차를 스스로 구성한다."
            : IntStream.range(0, item.sections().size())
                .mapToObj(i -> "  " + (i + 1) + ". " + item.sections().get(i))
                .collect(Collectors.joining("\n"));

        return """
            너는 운영 인수인계용 RUNBOOK을 쓰는 시니어 테크니컬 라이터다.
            아래 [컨텍스트](실제 소스코드·문서에서 검색된 조각)를 근거로 '%s' 가이드를
            인수인계 문서 수준으로 상세하고 정확하게 한국어 Markdown으로 작성한다.

            [문서 형식 — 반드시 준수]
            - 문서 맨 위에 '# %s' 제목과, 이 문서가 무엇을 다루는지 1~2문장 요약을 둔다.
            - 본문은 '## 1. 제목', '### 1-1. 제목'처럼 번호를 매긴 계층 섹션으로 구성한다.
            - 구조화된 정보(모듈/패키지, 클래스·역할, 테이블·컬럼, 엔드포인트, 상태·전이, 환경변수 등)는 반드시 **표**로 정리한다.
            - 아키텍처·요청 흐름·데이터 흐름은 ```mermaid``` flowchart 로, 상태 전이·승인 흐름은 ```mermaid``` stateDiagram-v2 로 그린다.
            - 디렉토리 구조·설정·명령어·코드 예시는 언어를 지정한 코드블록(```bash, ```java, ```yaml 등)으로 제시한다.
            - 핵심 규칙·전제조건·주의사항은 인용구로 강조한다(예: '> 핵심 규칙:', '> ⚠️ 주의:', '> 한눈에:').

            [내용 지침]
            - 대상 독자: %s. 이 독자가 바로 운영/개발에 착수할 수 있도록 '무엇을 → 어떻게 → 왜'를 함께 설명한다.
            - 상세 수준(%s): %s
            - 아래 목차를 '##' 섹션으로 순서대로 모두 포함하고, 각 섹션을 빠짐없이 충실히 채운다:
            %s
            - 각 섹션 끝에는 가능하면 '운영 시 주의점' 또는 '자주 겪는 문제'를 덧붙인다.
            - [컨텍스트]에 등장하는 **실제 식별자(클래스명·파일 경로·패키지·테이블·API 경로·명령어)를 그대로 인용**해 구체적으로 쓴다. 추상적 일반론은 피한다.
            - [컨텍스트]에 근거가 없는 내용은 지어내지 말고 'TODO: 확인 필요'로 표기한다. 해당 없는 항목은 '미해당'으로 명시한다.

            [가이드 목적/요청]
            %s

            [컨텍스트]
            %s
            """.formatted(
                item.title(), item.title(), audience,
                item.detailLevel().label(), item.detailLevel().guidance(),
                outline, item.prompt(), ctx);
    }

    /** AI 미설정 시 초안 — 목차를 지정했으면 섹션 골격까지 잡아 둔다. */
    private String stubMarkdown(TemplateItem item) {
        StringBuilder sb = new StringBuilder("# ").append(item.title()).append("\n\n")
            .append("> (초안) ").append(item.prompt()).append("\n>\n")
            .append("> AI 미설정 — 'ai' 프로파일로 실행하면 실제 생성됩니다. docs/AI-SETUP.md\n");
        if (!item.sections().isEmpty()) {
            sb.append('\n');
            for (String section : item.sections()) {
                sb.append("## ").append(section).append("\n\n_(작성 예정)_\n\n");
            }
        }
        return sb.toString();
    }

    private String deriveTitle(String prompt) {
        String t = prompt.strip();
        return t.length() > 80 ? t.substring(0, 80) : t;
    }
}
