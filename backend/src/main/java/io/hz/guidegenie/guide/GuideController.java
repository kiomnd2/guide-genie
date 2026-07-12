package io.hz.guidegenie.guide;

import io.hz.guidegenie.ai.AiOrchestrator;
import io.hz.guidegenie.common.SecurityUtils;
import io.hz.guidegenie.guide.dto.GuideDtos.CreateRequest;
import io.hz.guidegenie.guide.dto.GuideDtos.GenerateAccepted;
import io.hz.guidegenie.guide.dto.GuideDtos.GenerateRequest;
import io.hz.guidegenie.guide.dto.GuideDtos.Response;
import io.hz.guidegenie.guide.dto.GuideDtos.UpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/guides")
@RequiredArgsConstructor
public class GuideController {

    private final GuideService guideService;
    private final AiOrchestrator aiOrchestrator;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Response create(@PathVariable Long projectId, @Valid @RequestBody CreateRequest req) {
        Guide guide = guideService.createDraft(
            projectId, req.title(), req.contentMd(), SecurityUtils.currentUser(), null);
        return Response.from(guide);
    }

    @GetMapping
    public List<Response> list(@PathVariable Long projectId) {
        return guideService.findByProject(projectId).stream().map(Response::from).toList();
    }

    @GetMapping("/{guideId}")
    public Response get(@PathVariable Long projectId, @PathVariable Long guideId) {
        return Response.from(guideService.get(guideId));
    }

    @PutMapping("/{guideId}")
    public Response update(@PathVariable Long projectId, @PathVariable Long guideId,
                           @Valid @RequestBody UpdateRequest req) {
        return Response.from(
            guideService.update(guideId, req.title(), req.contentMd(), SecurityUtils.currentUser()));
    }

    @PostMapping("/{guideId}/publish")
    public Response publish(@PathVariable Long projectId, @PathVariable Long guideId) {
        return Response.from(guideService.publish(guideId));
    }

    /** 프롬프트 기반 가이드 생성(비동기). job id 반환. */
    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public GenerateAccepted generate(@PathVariable Long projectId,
                                     @Valid @RequestBody GenerateRequest req) {
        String jobId = UUID.randomUUID().toString();
        aiOrchestrator.generateGuide(projectId, req.prompt(), SecurityUtils.currentUser(), jobId);
        return new GenerateAccepted(jobId);
    }
}
