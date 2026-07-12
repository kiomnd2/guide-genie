package io.hz.guidegenie.source.connector;

import io.hz.guidegenie.source.SourceType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConnectorRegistry {

    private final List<SourceConnector> connectors;

    public SourceConnector forType(SourceType type) {
        return connectors.stream()
            .filter(c -> c.supports(type))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No connector for type: " + type));
    }
}
