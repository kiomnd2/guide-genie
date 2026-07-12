package io.hz.guidegenie.source.connector;

/**
 * 커넥터가 원본 시스템에서 가져온 문서 1건 (정제 전/후 공용 표현).
 *
 * @param externalId 원본 시스템 식별자
 * @param title      제목
 * @param content    본문 (HTML→텍스트, 코드 파싱 등 정제된 결과)
 * @param url        원본 링크
 */
public record RawDocument(
    String externalId,
    String title,
    String content,
    String url
) {
}
