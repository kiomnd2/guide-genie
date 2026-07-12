package io.hz.guidegenie.api.template;

import io.hz.guidegenie.common.SecurityUtils;
import io.hz.guidegenie.guide.application.service.CategoryService;
import io.hz.guidegenie.guide.application.service.GuideTemplateService;
import io.hz.guidegenie.guide.domain.GuideTemplate;
import io.hz.guidegenie.guide.domain.TemplateItem;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Response create(@PathVariable Long projectId, @Valid @RequestBody UpsertRequest req) {
        validateCategories(projectId, req);
        return Response.from(templateService.create(
            projectId, req.name(), toItems(req), SecurityUtils.currentUser()));
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

    /** 일괄 생성 실행(비동기). 트리거된 항목 수 반환. */
    @PostMapping("/{templateId}/run")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RunResult run(@PathVariable Long projectId, @PathVariable Long templateId) {
        return new RunResult(templateService.run(templateId, SecurityUtils.currentUser()));
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
            .map(i -> new TemplateItem(i.title(), i.prompt(), i.categoryId()))
            .toList();
    }

    public record ItemDto(@NotBlank String title, @NotBlank String prompt, Long categoryId) {}

    public record UpsertRequest(@NotBlank String name, List<ItemDto> items) {}

    public record RunResult(int triggered) {}

    public record Response(Long id, String name, List<ItemDto> items) {
        static Response from(GuideTemplate t) {
            List<ItemDto> items = t.getItems().stream()
                .map(i -> new ItemDto(i.title(), i.prompt(), i.categoryId()))
                .toList();
            return new Response(t.getId(), t.getName(), items);
        }
    }
}
