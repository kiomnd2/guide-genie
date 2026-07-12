package io.hz.guidegenie.rag.application.port.in;

import java.util.Map;

/**
 * RAG 검색 결과 청크.
 *
 * @param chunkText 청크 본문
 * @param metadata  출처 메타데이터(guide_id, title, anchor, source_url …)
 * @param score     유사도 점수
 */
public record RetrievedChunk(
    String chunkText,
    Map<String, Object> metadata,
    double score
) {
}
