package io.hz.guidegenie.project;

import io.hz.guidegenie.common.SecurityUtils;
import io.hz.guidegenie.project.dto.ProjectDtos.CreateRequest;
import io.hz.guidegenie.project.dto.ProjectDtos.Response;
import io.hz.guidegenie.project.dto.ProjectDtos.UpdateRequest;
import jakarta.validation.Valid;
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
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Response create(@Valid @RequestBody CreateRequest req) {
        return Response.from(
            projectService.create(req.name(), req.description(), SecurityUtils.currentUser()));
    }

    @GetMapping
    public List<Response> list() {
        return projectService.findAll().stream().map(Response::from).toList();
    }

    @GetMapping("/{id}")
    public Response get(@PathVariable Long id) {
        return Response.from(projectService.get(id));
    }

    @PutMapping("/{id}")
    public Response update(@PathVariable Long id, @Valid @RequestBody UpdateRequest req) {
        return Response.from(projectService.update(id, req.name(), req.description()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        projectService.delete(id);
    }
}
