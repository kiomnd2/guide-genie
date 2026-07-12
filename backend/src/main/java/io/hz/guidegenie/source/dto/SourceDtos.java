package io.hz.guidegenie.source.dto;

import io.hz.guidegenie.source.SourceConnection;
import io.hz.guidegenie.source.SourceType;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Map;

public final class SourceDtos {

    private SourceDtos() {
    }

    public record CreateConnectionRequest(
        @NotNull SourceType type,
        @NotNull Map<String, Object> config,
        String token
    ) {
    }

    public record ConnectionResponse(
        Long id,
        Long projectId,
        SourceType type,
        Map<String, Object> config,
        OffsetDateTime lastSyncedAt
    ) {
        public static ConnectionResponse from(SourceConnection c) {
            return new ConnectionResponse(
                c.getId(), c.getProjectId(), c.getType(), c.getConfig(), c.getLastSyncedAt());
        }
    }
}
