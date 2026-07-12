package io.hz.guidegenie.source.connector;

import io.hz.guidegenie.source.SourceConnection;
import io.hz.guidegenie.source.SourceType;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 데이터 소스 커넥터. 구현체는 {@link #supports(SourceType)} 로 자신이 담당하는 타입을 알린다.
 */
public interface SourceConnector {

    boolean supports(SourceType type);

    /** 연결 검증(자격증명/권한 확인). */
    boolean testConnection(SourceConnection connection);

    /** 전체 수집(Full Sync). */
    List<RawDocument> fetchAll(SourceConnection connection);

    /** 증분 수집(Incremental Sync) — {@code since} 이후 변경분만. */
    List<RawDocument> fetchSince(SourceConnection connection, OffsetDateTime since);
}
