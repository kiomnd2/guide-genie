package io.hz.guidegenie.guide.domain;

/**
 * 가이드 상세 수준. 템플릿 항목마다 지정하며, AI 생성 시 분량·깊이 지침으로 프롬프트에 반영된다.
 */
public enum DetailLevel {

    BRIEF("간단", "핵심만 간결하게. 각 섹션을 2~3문장으로 요약한다."),
    STANDARD("표준", "각 섹션을 문단과 목록으로 설명하고, 이해를 돕는 예시를 곁들인다."),
    DETAILED("상세",
        "각 섹션을 배경 설명 → 단계별 절차 → 구체 예시(명령어·코드·설정) → 주의사항·트러블슈팅까지 깊이 있게 서술한다.");

    private final String label;
    private final String guidance;

    DetailLevel(String label, String guidance) {
        this.label = label;
        this.guidance = guidance;
    }

    /** UI·프롬프트에 노출할 한국어 라벨. */
    public String label() {
        return label;
    }

    /** AI 프롬프트에 삽입할 작성 지침. */
    public String guidance() {
        return guidance;
    }
}
