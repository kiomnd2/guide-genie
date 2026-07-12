package io.hz.guidegenie.source.connector;

import io.hz.guidegenie.source.SourceConnection;
import io.hz.guidegenie.source.SourceType;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Atlassian Cloud Confluence 커넥터.
 * REST API v3 + API Token(또는 OAuth 2.0 3LO)로 스페이스 내 페이지를 수집한다.
 */
@Slf4j
@Component
public class ConfluenceConnector implements SourceConnector {

    @Override
    public boolean supports(SourceType type) {
        return type == SourceType.CONFLUENCE;
    }

    @Override
    public boolean testConnection(SourceConnection connection) {
        // TODO: GET /wiki/rest/api/space 로 자격증명/스페이스 접근 검증
        log.info("[Confluence] testConnection connectionId={}", connection.getId());
        return true;
    }

    @Override
    public List<RawDocument> fetchAll(SourceConnection connection) {
        // TODO: 스페이스 페이지 전체 수집(storage 포맷) → HTML→텍스트 정제
        log.info("[Confluence] fetchAll connectionId={}", connection.getId());
        return List.of();
    }

    @Override
    public List<RawDocument> fetchSince(SourceConnection connection, OffsetDateTime since) {
        // TODO: CQL lastmodified >= since 로 증분 수집
        log.info("[Confluence] fetchSince connectionId={} since={}", connection.getId(), since);
        return List.of();
    }
}
