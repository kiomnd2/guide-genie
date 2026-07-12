package io.hz.guidegenie.project.dto;

import io.hz.guidegenie.project.Project;
import jakarta.validation.constraints.NotBlank;

public final class ProjectDtos {

    private ProjectDtos() {
    }

    public record CreateRequest(
        @NotBlank String name,
        String description
    ) {
    }

    public record UpdateRequest(
        @NotBlank String name,
        String description
    ) {
    }

    public record Response(
        Long id,
        String name,
        String description,
        String owner
    ) {
        public static Response from(Project p) {
            return new Response(p.getId(), p.getName(), p.getDescription(), p.getOwner());
        }
    }
}
