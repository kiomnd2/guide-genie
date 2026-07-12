package io.hz.guidegenie.api.guide;

import io.hz.guidegenie.common.SecurityUtils;
import io.hz.guidegenie.guide.application.service.CategoryService;
import io.hz.guidegenie.guide.application.service.GuideGenerationService;
import io.hz.guidegenie.guide.application.service.GuideService;
import io.hz.guidegenie.guide.domain.Guide;
import io.hz.guidegenie.guide.domain.GuideStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
    private final GuideGenerationService generationService;
    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Response create(@PathVariable Long projectId, @Valid @RequestBody CreateRequest req) {
        categoryService.assertInProject(req.categoryId(), projectId);
        return Response.from(guideService.createDraft(
            projectId, req.title(), req.categoryId(), req.contentMd(), SecurityUtils.currentUser(), null));
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
        categoryService.assertInProject(req.categoryId(), projectId);
        return Response.from(guideService.update(
            guideId, req.title(), req.categoryId(), req.contentMd(), SecurityUtils.currentUser()));
    }

    @PostMapping("/{guideId}/publish")
    public Response publish(@PathVariable Long projectId, @PathVariable Long guideId) {
        return Response.from(guideService.publish(guideId));
    }

    /** 분류 이동(드래그 앤 드롭). categoryId=null → 미분류. */
    @PatchMapping("/{guideId}/category")
    public Response move(@PathVariable Long projectId, @PathVariable Long guideId,
                         @RequestBody MoveRequest req) {
        categoryService.assertInProject(req.categoryId(), projectId);
        return Response.from(guideService.moveToCategory(guideId, req.categoryId()));
    }

    /** 프롬프트 기반 생성(비동기). job id 반환. */
    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public GenerateAccepted generate(@PathVariable Long projectId,
                                     @Valid @RequestBody GenerateRequest req) {
        String jobId = UUID.randomUUID().toString();
        generationService.generate(projectId, req.prompt(), SecurityUtils.currentUser(), jobId);
        return new GenerateAccepted(jobId);
    }

    public record CreateRequest(@NotBlank String title, Long categoryId, String contentMd) {}

    public record UpdateRequest(@NotBlank String title, Long categoryId, @NotBlank String contentMd) {}

    public record MoveRequest(Long categoryId) {}

    public record GenerateRequest(@NotBlank String prompt) {}

    public record GenerateAccepted(String jobId) {}

    public record Response(Long id, Long projectId, String title, Long categoryId,
                           GuideStatus status, String createdBy) {
        static Response from(Guide g) {
            return new Response(g.getId(), g.getProjectId(), g.getTitle(), g.getCategoryId(),
                g.getStatus(), g.getCreatedBy());
        }
    }
}
