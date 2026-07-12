package io.hz.guidegenie.source.application.port.out;

/**
 * 커넥터가 원본에서 가져온 문서 1건(정제 후 표현).
 *
 * @param externalId 원본 식별자
 * @param title      제목
 * @param content    본문(HTML→텍스트/코드 파싱 등 정제 결과)
 * @param url        원본 링크
 */
public record RawDocument(String externalId, String title, String content, String url) {
}
