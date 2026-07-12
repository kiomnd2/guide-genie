package io.hz.guidegenie.source.application.port.out;

import io.hz.guidegenie.source.domain.SourceConnection;
import io.hz.guidegenie.source.domain.SourceType;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 아웃바운드 포트(게이트웨이) — 데이터 소스 커넥터. 구현체는 adapter.out.external의 *Client.
 * 여러 구현이 타입별로 존재하며 ConnectorRegistry가 선택한다.
 */
public interface SourceConnectorPort {

    boolean supports(SourceType type);

    boolean testConnection(SourceConnection connection);

    List<RawDocument> fetchAll(SourceConnection connection);

    List<RawDocument> fetchSince(SourceConnection connection, OffsetDateTime since);
}
