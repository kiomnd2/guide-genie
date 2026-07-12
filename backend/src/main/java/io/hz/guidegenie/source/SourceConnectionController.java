package io.hz.guidegenie.source;

import io.hz.guidegenie.source.dto.SourceDtos.ConnectionResponse;
import io.hz.guidegenie.source.dto.SourceDtos.CreateConnectionRequest;
import jakarta.validation.Valid;
import java.util.List;
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
    public ConnectionResponse create(@PathVariable Long projectId,
                                     @Valid @RequestBody CreateConnectionRequest req) {
        return ConnectionResponse.from(
            connectionService.create(projectId, req.type(), req.config(), req.token()));
    }

    @GetMapping
    public List<ConnectionResponse> list(@PathVariable Long projectId) {
        return connectionService.findByProject(projectId).stream()
            .map(ConnectionResponse::from)
            .toList();
    }

    @PostMapping("/{cid}/sync")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sync(@PathVariable Long projectId, @PathVariable Long cid) {
        connectionService.triggerSync(cid);
    }
}
