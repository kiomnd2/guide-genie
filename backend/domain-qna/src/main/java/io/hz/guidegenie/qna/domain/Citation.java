package io.hz.guidegenie.qna.domain;

/**
 * 답변 출처. 가이드 문서명 + 섹션(앵커) + 필요 시 원본 소스 링크.
 *
 * @param guideId    참조 가이드 id
 * @param guideTitle 가이드 제목
 * @param section    섹션/헤딩 앵커
 * @param sourceUrl  원본 소스 링크(있을 경우)
 */
public record Citation(Long guideId, String guideTitle, String section, String sourceUrl) {
}
