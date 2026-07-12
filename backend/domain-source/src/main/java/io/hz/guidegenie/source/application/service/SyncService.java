package io.hz.guidegenie.source.application.service;

import io.hz.guidegenie.rag.application.port.in.IndexPort;
import io.hz.guidegenie.rag.domain.RefType;
import io.hz.guidegenie.source.application.port.out.RawDocument;
import io.hz.guidegenie.source.application.port.out.SourceConnectionRepositoryPort;
import io.hz.guidegenie.source.application.port.out.SourceConnectorPort;
import io.hz.guidegenie.source.application.port.out.SourceDocumentRepositoryPort;
import io.hz.guidegenie.source.domain.SourceConnection;
import io.hz.guidegenie.source.domain.SourceDocument;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 소스 수집 파이프라인: 커넥터 fetch → SourceDocument upsert → RAG 색인(IndexPort).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {

    private final SourceConnectionRepositoryPort connectionRepository;
    private final SourceDocumentRepositoryPort documentRepository;
    private final ConnectorRegistry connectorRegistry;
    private final IndexPort ragIndex;

    /** 단일 연결 동기화(수동 트리거/스케줄러). 비동기. */
    @Async
    @Transactional
    public void sync(Long connectionId, boolean full) {
        SourceConnection connection = connectionRepository.findById(connectionId)
            .orElseThrow(() -> new IllegalArgumentException("connection not found: " + connectionId));

        SourceConnectorPort connector = connectorRegistry.forType(connection.getType());
        OffsetDateTime now = OffsetDateTime.now();

        List<RawDocument> docs = (full || connection.getLastSyncedAt() == null)
            ? connector.fetchAll(connection)
            : connector.fetchSince(connection, connection.getLastSyncedAt());

        log.info("[Sync] connectionId={} type={} full={} fetched={}",
            connectionId, connection.getType(), full, docs.size());

        for (RawDocument raw : docs) {
            SourceDocument doc = documentRepository
                .findByConnectionIdAndExternalId(connectionId, raw.externalId())
                .map(existing -> {
                    existing.update(raw.title(), raw.content(), raw.url(), now);
                    return existing;
                })
                .orElseGet(() -> new SourceDocument(
                    connectionId, raw.externalId(), raw.title(), raw.content(), raw.url(), now));

            SourceDocument saved = documentRepository.save(doc);
            ragIndex.index(connection.getProjectId(), RefType.SOURCE, saved.getId(),
                saved.getTitle(), saved.getContent());
        }

        connection.markSynced(now);
    }

    /** 모든 연결 증분 동기화(워커 스케줄러가 호출). */
    public void syncAllIncremental() {
        connectionRepository.findAll().forEach(c -> sync(c.getId(), false));
    }
}
