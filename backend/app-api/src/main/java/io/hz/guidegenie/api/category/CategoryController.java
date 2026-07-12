package io.hz.guidegenie.api.category;

import io.hz.guidegenie.guide.application.service.CategoryService;
import io.hz.guidegenie.guide.domain.Category;
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

@RestController
@RequestMapping("/api/projects/{projectId}/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Response create(@PathVariable Long projectId, @Valid @RequestBody CreateRequest req) {
        return Response.from(categoryService.create(projectId, req.name(), req.parentId()));
    }

    @GetMapping
    public List<Response> list(@PathVariable Long projectId) {
        return categoryService.findByProject(projectId).stream().map(Response::from).toList();
    }

    @PutMapping("/{categoryId}")
    public Response rename(@PathVariable Long projectId, @PathVariable Long categoryId,
                           @Valid @RequestBody RenameRequest req) {
        return Response.from(categoryService.rename(categoryId, req.name()));
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long projectId, @PathVariable Long categoryId) {
        categoryService.delete(categoryId);
    }

    public record CreateRequest(@NotBlank String name, Long parentId) {}

    public record RenameRequest(@NotBlank String name) {}

    public record Response(Long id, Long parentId, String name) {
        static Response from(Category c) {
            return new Response(c.getId(), c.getParentId(), c.getName());
        }
    }
}
