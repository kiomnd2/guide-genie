package io.hz.guidegenie.source.adapter.out.external;

import io.hz.guidegenie.source.application.port.out.RawDocument;
import io.hz.guidegenie.source.application.port.out.SourceConnectorPort;
import io.hz.guidegenie.source.domain.SourceConnection;
import io.hz.guidegenie.source.domain.SourceType;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** GitHub 커넥터 (GitHub App 또는 PAT — 코드·README·Wiki·PR 설명 수집). */
@Slf4j
@Component
public class GitHubClient implements SourceConnectorPort {

    @Override
    public boolean supports(SourceType type) {
        return type == SourceType.GITHUB;
    }

    @Override
    public boolean testConnection(SourceConnection connection) {
        // TODO: GET /repos/{owner}/{repo} 로 접근 권한 검증
        log.info("[GitHub] testConnection connectionId={}", connection.getId());
        return true;
    }

    @Override
    public List<RawDocument> fetchAll(SourceConnection connection) {
        // TODO: git tree 순회 + 확장자/경로 필터, README/PR/Wiki 수집, 코드 파싱
        log.info("[GitHub] fetchAll connectionId={}", connection.getId());
        return List.of();
    }

    @Override
    public List<RawDocument> fetchSince(SourceConnection connection, OffsetDateTime since) {
        // TODO: commits since 기반 변경 파일 증분 수집(또는 Webhook)
        log.info("[GitHub] fetchSince connectionId={} since={}", connection.getId(), since);
        return List.of();
    }
}
