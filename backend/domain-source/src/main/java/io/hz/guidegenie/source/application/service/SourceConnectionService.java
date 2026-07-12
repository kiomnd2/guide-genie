package io.hz.guidegenie.source.application.service;

import io.hz.guidegenie.common.NotFoundException;
import io.hz.guidegenie.common.TokenCipher;
import io.hz.guidegenie.source.application.port.out.SourceConnectionRepositoryPort;
import io.hz.guidegenie.source.domain.SourceConnection;
import io.hz.guidegenie.source.domain.SourceType;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SourceConnectionService {

    private final SourceConnectionRepositoryPort connectionRepository;
    private final ConnectorRegistry connectorRegistry;
    private final TokenCipher tokenCipher;
    private final SyncService syncService;

    @Transactional
    public SourceConnection create(Long projectId, SourceType type, Map<String, Object> config,
                                   String rawToken) {
        SourceConnection connection = connectionRepository.save(
            new SourceConnection(projectId, type, config, tokenCipher.encrypt(rawToken)));

        connectorRegistry.forType(type).testConnection(connection);
        syncService.sync(connection.getId(), true); // 초기 전체 수집(비동기)
        return connection;
    }

    @Transactional(readOnly = true)
    public List<SourceConnection> findByProject(Long projectId) {
        return connectionRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public SourceConnection get(Long id) {
        return connectionRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("SourceConnection not found: " + id));
    }

    public void triggerSync(Long connectionId) {
        get(connectionId); // 존재 검증
        syncService.sync(connectionId, false);
    }
}
