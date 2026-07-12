package io.hz.guidegenie.source.adapter.out.external;

import io.hz.guidegenie.source.application.port.out.RawDocument;
import io.hz.guidegenie.source.application.port.out.SourceConnectorPort;
import io.hz.guidegenie.source.domain.SourceConnection;
import io.hz.guidegenie.source.domain.SourceType;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Atlassian Cloud Confluence 커넥터 (REST API v3 + API Token/OAuth 2.0 3LO). */
@Slf4j
@Component
public class ConfluenceClient implements SourceConnectorPort {

    @Override
    public boolean supports(SourceType type) {
        return type == SourceType.CONFLUENCE;
    }

    @Override
    public boolean testConnection(SourceConnection connection) {
        // TODO: GET /wiki/rest/api/space 로 검증
        log.info("[Confluence] testConnection connectionId={}", connection.getId());
        return true;
    }

    @Override
    public List<RawDocument> fetchAll(SourceConnection connection) {
        // TODO: 스페이스 페이지 전체 수집(storage) → HTML→텍스트 정제
        log.info("[Confluence] fetchAll connectionId={}", connection.getId());
        return List.of();
    }

    @Override
    public List<RawDocument> fetchSince(SourceConnection connection, OffsetDateTime since) {
        // TODO: CQL lastmodified >= since 증분 수집
        log.info("[Confluence] fetchSince connectionId={} since={}", connection.getId(), since);
        return List.of();
    }
}
