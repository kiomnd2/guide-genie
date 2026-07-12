package io.hz.guidegenie.guide.domain;

/**
 * 가이드 세트 템플릿의 항목 1개. 실행 시 항목마다 가이드 초안 1개를 생성한다.
 *
 * @param title      생성될 가이드 제목
 * @param prompt     AI 생성 프롬프트
 * @param categoryId 배치할 분류(선택, null = 미분류)
 */
public record TemplateItem(String title, String prompt, Long categoryId) {
}
