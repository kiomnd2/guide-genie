package io.hz.guidegenie.rag.application.port.in;

import java.util.List;

/**
 * 인바운드 포트 — RAG 검색. 다른 도메인(qna, guide 생성)이 호출한다.
 */
public interface SearchPort {

    /** 프로젝트 범위에서 질의와 유사한 상위 topK 청크를 반환한다. */
    List<RetrievedChunk> search(Long projectId, String query, int topK);
}
