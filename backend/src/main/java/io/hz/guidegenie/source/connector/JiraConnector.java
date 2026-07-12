package io.hz.guidegenie.source.connector;

import io.hz.guidegenie.source.SourceConnection;
import io.hz.guidegenie.source.SourceType;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Atlassian Cloud Jira 커넥터.
 * REST API v3 + API Token(또는 OAuth 2.0 3LO)로 이슈를 수집한다.
 */
@Slf4j
@Component
public class JiraConnector implements SourceConnector {

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
        // TODO: JQL 페이지네이션(GET /rest/api/3/search)으로 이슈 전체 수집,
        //       ADF(본문) → 텍스트 정제 후 RawDocument 매핑
        log.info("[Jira] fetchAll connectionId={}", connection.getId());
        return List.of();
    }

    @Override
    public List<RawDocument> fetchSince(SourceConnection connection, OffsetDateTime since) {
        // TODO: JQL updated >= since 로 증분 수집
        log.info("[Jira] fetchSince connectionId={} since={}", connection.getId(), since);
        return List.of();
    }
}
