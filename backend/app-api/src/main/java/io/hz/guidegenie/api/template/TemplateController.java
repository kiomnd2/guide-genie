package io.hz.guidegenie.api.template;

import io.hz.guidegenie.common.SecurityUtils;
import io.hz.guidegenie.guide.application.service.CategoryService;
import io.hz.guidegenie.guide.application.service.GenerationJobTracker;
import io.hz.guidegenie.guide.application.service.GuideTemplateService;
import io.hz.guidegenie.guide.application.service.GuideTemplateService.RunHandle;
import io.hz.guidegenie.guide.domain.DetailLevel;
import io.hz.guidegenie.guide.domain.GuideTemplate;
import io.hz.guidegenie.guide.domain.TemplateItem;
import io.hz.guidegenie.source.application.service.SourceConnectionService;
import io.hz.guidegenie.source.domain.SourceConnection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 가이드 세트 템플릿 관리 + 일괄 생성. */
@RestController
@RequestMapping("/api/projects/{projectId}/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final GuideTemplateService templateService;
    private final CategoryService categoryService;
    private final SourceConnectionService connectionService;
    private final GenerationJobTracker jobTracker;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Response create(@PathVariable Long projectId, @Valid @RequestBody UpsertRequest req) {
        validateCategories(projectId, req);
        return Response.from(templateService.create(
            projectId, req.name(), toItems(req), SecurityUtils.currentUser()));
    }

    /** 연동 정보 기준 템플릿 자동 생성 — 등록된 소스별 추천 항목으로 템플릿을 만든다. */
    @PostMapping("/auto")
    @ResponseStatus(HttpStatus.CREATED)
    public Response auto(@PathVariable Long projectId) {
        List<SourceConnection> connections = connectionService.findByProject(projectId);
        List<TemplateItem> items = ConnectionTemplatePresets.build(connections);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("연동된 소스가 없습니다. 먼저 소스를 연동한 뒤 자동 생성하세요.");
        }
        return Response.from(templateService.create(
            projectId, "연동 기반 가이드 세트 (자동)", items, SecurityUtils.currentUser()));
    }

    /** GitHub 기준 운영 인수인계 RUNBOOK 세트 자동 생성 — 챕터별 상세 항목으로 템플릿을 만든다. */
    @PostMapping("/runbook")
    @ResponseStatus(HttpStatus.CREATED)
    public Response runbook(@PathVariable Long projectId) {
        List<SourceConnection> connections = connectionService.findByProject(projectId);
        List<TemplateItem> items = RunbookTemplatePresets.build(connections);
        if (items.isEmpty()) {
            throw new IllegalArgumentException(
                "RUNBOOK 자동 생성은 GitHub 연동이 필요합니다. 먼저 GitHub 소스를 연동하세요.");
        }
        return Response.from(templateService.create(
            projectId, "운영 인수인계 RUNBOOK 세트", items, SecurityUtils.currentUser()));
    }

    @GetMapping
    public List<Response> list(@PathVariable Long projectId) {
        return templateService.findByProject(projectId).stream().map(Response::from).toList();
    }

    @GetMapping("/{templateId}")
    public Response get(@PathVariable Long projectId, @PathVariable Long templateId) {
        return Response.from(templateService.get(templateId));
    }

    @PutMapping("/{templateId}")
    public Response update(@PathVariable Long projectId, @PathVariable Long templateId,
                           @Valid @RequestBody UpsertRequest req) {
        validateCategories(projectId, req);
        return Response.from(templateService.update(templateId, req.name(), toItems(req)));
    }

    @DeleteMapping("/{templateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long projectId, @PathVariable Long templateId) {
        templateService.delete(templateId);
    }

    /** 일괄 생성 실행(비동기). 진행 조회용 jobId + 총 항목 수 반환. */
    @PostMapping("/{templateId}/run")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RunHandle run(@PathVariable Long projectId, @PathVariable Long templateId) {
        return templateService.run(templateId, SecurityUtils.currentUser());
    }

    /** 일괄 생성 진행 상황 조회(폴링). */
    @GetMapping("/runs/{jobId}")
    public GenerationJobTracker.Progress runStatus(@PathVariable Long projectId,
                                                   @PathVariable String jobId) {
        return jobTracker.snapshot(jobId);
    }

    private void validateCategories(Long projectId, UpsertRequest req) {
        if (req.items() != null) {
            req.items().forEach(i -> categoryService.assertInProject(i.categoryId(), projectId));
        }
    }

    private List<TemplateItem> toItems(UpsertRequest req) {
        if (req.items() == null) {
            return List.of();
        }
        return req.items().stream()
            .map(i -> new TemplateItem(
                i.title(), i.prompt(), i.categoryId(), i.audience(), cleanSections(i.sections()), i.detailLevel()))
            .toList();
    }

    /** 빈 문자열·공백 목차는 제거하고 트림 — UI에서 줄바꿈으로 입력한 목차를 정리한다. */
    private static List<String> cleanSections(List<String> sections) {
        if (sections == null) {
            return List.of();
        }
        return sections.stream().map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    public record ItemDto(
        @NotBlank String title,
        @NotBlank String prompt,
        Long categoryId,
        String audience,
        List<String> sections,
        DetailLevel detailLevel) {}

    public record UpsertRequest(@NotBlank String name, List<ItemDto> items) {}

    public record Response(Long id, String name, List<ItemDto> items) {
        static Response from(GuideTemplate t) {
            List<ItemDto> items = t.getItems().stream()
                .map(i -> new ItemDto(
                    i.title(), i.prompt(), i.categoryId(), i.audience(), i.sections(), i.detailLevel()))
                .toList();
            return new Response(t.getId(), t.getName(), items);
        }
    }
}
