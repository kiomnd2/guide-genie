package io.hz.guidegenie.source;

import io.hz.guidegenie.embedding.EmbeddingService;
import io.hz.guidegenie.source.connector.ConnectorRegistry;
import io.hz.guidegenie.source.connector.RawDocument;
import io.hz.guidegenie.source.connector.SourceConnector;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 소스 수집 파이프라인: 커넥터 fetch → SourceDocument 저장(upsert) → 재임베딩.
 * 초기 전체 수집과 주기적 증분 수집을 담당한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {

    private final SourceConnectionRepository connectionRepository;
    private final SourceDocumentRepository documentRepository;
    private final ConnectorRegistry connectorRegistry;
    private final EmbeddingService embeddingService;

    /** 단일 연결 동기화(수동 트리거 또는 스케줄러에서 호출). 비동기 처리. */
    @Async
    @Transactional
    public void sync(Long connectionId, boolean full) {
        SourceConnection connection = connectionRepository.findById(connectionId)
            .orElseThrow(() -> new IllegalArgumentException("connection not found: " + connectionId));

        SourceConnector connector = connectorRegistry.forType(connection.getType());
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
            // 정제 → 청크 분할 → 임베딩
            embeddingService.embedSourceDocument(saved);
        }

        connection.markSynced(now);
    }

    /** 증분 동기화 스케줄러. 주기는 guidegenie.sync.cron 설정. */
    @Scheduled(cron = "${guidegenie.sync.cron}")
    public void scheduledIncrementalSync() {
        connectionRepository.findAll()
            .forEach(c -> sync(c.getId(), false));
    }
}
