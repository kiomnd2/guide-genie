package io.hz.guidegenie.source.application.service;

import io.hz.guidegenie.source.application.port.out.SourceConnectorPort;
import io.hz.guidegenie.source.domain.SourceType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 타입별 커넥터(SourceConnectorPort 구현) 선택. */
@Component
@RequiredArgsConstructor
public class ConnectorRegistry {

    private final List<SourceConnectorPort> connectors;

    public SourceConnectorPort forType(SourceType type) {
        return connectors.stream()
            .filter(c -> c.supports(type))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No connector for type: " + type));
    }
}
