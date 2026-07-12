package io.hz.guidegenie.rag.application.port.in;

import io.hz.guidegenie.rag.domain.RefType;

/**
 * 인바운드 포트 — 색인(임베딩). 다른 도메인(guide/source)이 게시·동기화 시 호출한다.
 * (도메인 간 downward 의존: guide/source → rag)
 */
public interface IndexPort {

    /** 주어진 텍스트를 청크 분할해 색인한다. 같은 (refType, refId)는 재색인 전 기존 청크를 제거한다. */
    void index(Long projectId, RefType refType, Long refId, String title, String text);

    /** 특정 출처의 색인을 제거한다. */
    void removeByRef(RefType refType, Long refId);
}
