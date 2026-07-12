package io.hz.guidegenie.source.adapter.out.external;

import io.hz.guidegenie.source.application.port.out.RawDocument;
import io.hz.guidegenie.source.application.port.out.SourceConnectorPort;
import io.hz.guidegenie.source.domain.SourceConnection;
import io.hz.guidegenie.source.domain.SourceType;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Atlassian Cloud Jira 커넥터 (REST API v3 + API Token/OAuth 2.0 3LO). */
@Slf4j
@Component
public class JiraClient implements SourceConnectorPort {

    @Override
    public boolean supports(SourceType type) {
        return type == SourceType.JIRA;
    }

    @Override
    public boolean testConnection(SourceConnection connection) {
        // TODO: GET /rest/api/3/myself 로 자격증명 검증
        log.info("[Jira] testConnection connectionId={}", connection.getId());
        return true;
    }

    @Override
    public List<RawDocument> fetchAll(SourceConnection connection) {
        // TODO: JQL 페이지네이션(GET /rest/api/3/search) → ADF 본문 정제 → RawDocument
        log.info("[Jira] fetchAll connectionId={}", connection.getId());
        return List.of();
    }

    @Override
    public List<RawDocument> fetchSince(SourceConnection connection, OffsetDateTime since) {
        // TODO: JQL updated >= since 증분 수집
        log.info("[Jira] fetchSince connectionId={} since={}", connection.getId(), since);
        return List.of();
    }
}
