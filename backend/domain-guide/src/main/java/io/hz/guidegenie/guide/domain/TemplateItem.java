package io.hz.guidegenie.guide.domain;

import java.util.List;

/**
 * 가이드 세트 템플릿의 항목 1개. 실행 시 항목마다 가이드 초안 1개를 생성한다.
 *
 * <p>{@code audience}·{@code sections}·{@code detailLevel} 은 AI가 <b>상세한</b> 가이드를 쓰도록
 * 프롬프트를 구체화하는 필드다. JSONB 저장이라 이전에 저장된(3-필드) 항목은 이 값들이 비어 있어도
 * 정상 역직렬화된다(compact 생성자가 기본값으로 보정).
 *
 * @param title       생성될 가이드 제목
 * @param prompt      가이드의 목적·요청(무엇을 다룰지)
 * @param categoryId  배치할 분류(선택, null = 미분류)
 * @param audience    대상 독자(선택) — 예) 신규 입사자, 운영 담당자
 * @param sections    포함할 목차(섹션) 제목 목록(선택) — 지정 시 AI가 이 구조를 그대로 채운다
 * @param detailLevel 상세 수준(분량·깊이). null 이면 {@link DetailLevel#STANDARD}
 */
public record TemplateItem(
    String title,
    String prompt,
    Long categoryId,
    String audience,
    List<String> sections,
    DetailLevel detailLevel
) {

    public TemplateItem {
        sections = sections == null ? List.of() : List.copyOf(sections);
        detailLevel = detailLevel == null ? DetailLevel.STANDARD : detailLevel;
    }

    /** 하위호환 — 상세 필드 없이 제목·프롬프트·분류만 지정. */
    public TemplateItem(String title, String prompt, Long categoryId) {
        this(title, prompt, categoryId, null, List.of(), DetailLevel.STANDARD);
    }
}
