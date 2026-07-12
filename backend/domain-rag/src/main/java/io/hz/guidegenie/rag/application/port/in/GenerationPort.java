package io.hz.guidegenie.rag.application.port.in;

/**
 * 인바운드 포트 — LLM 텍스트 생성. 다른 도메인(guide 생성, qna)이 호출한다.
 * provider(Gemini 등) 의존은 domain-rag 구현체에만 격리된다.
 */
public interface GenerationPort {

    /** AI 사용 가능 여부(모델 빈이 구성돼 있는지 = 'ai' 프로파일 활성 여부). */
    boolean enabled();

    /** 프롬프트로 텍스트를 생성한다. {@link #enabled()}가 false면 사용하지 말 것(호출 시 예외). */
    String generate(String prompt);
}
