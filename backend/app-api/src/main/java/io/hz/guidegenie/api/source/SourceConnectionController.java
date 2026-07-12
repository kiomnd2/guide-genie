package io.hz.guidegenie.api.source;

import io.hz.guidegenie.source.application.service.SourceConnectionService;
import io.hz.guidegenie.source.domain.SourceConnection;
import io.hz.guidegenie.source.domain.SourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/connections")
@RequiredArgsConstructor
public class SourceConnectionController {

    private final SourceConnectionService connectionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Response create(@PathVariable Long projectId, @Valid @RequestBody CreateRequest req) {
        return Response.from(
            connectionService.create(projectId, req.type(), req.config(), req.token()));
    }

    @GetMapping
    public List<Response> list(@PathVariable Long projectId) {
        return connectionService.findByProject(projectId).stream().map(Response::from).toList();
    }

    @PostMapping("/{cid}/sync")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sync(@PathVariable Long projectId, @PathVariable Long cid) {
        connectionService.triggerSync(cid);
    }

    public record CreateRequest(@NotNull SourceType type, @NotNull Map<String, Object> config,
                                String token) {}

    public record Response(Long id, Long projectId, SourceType type, Map<String, Object> config,
                           OffsetDateTime lastSyncedAt) {
        static Response from(SourceConnection c) {
            return new Response(c.getId(), c.getProjectId(), c.getType(), c.getConfig(),
                c.getLastSyncedAt());
        }
    }
}
