package io.hz.guidegenie.guide.dto;

import io.hz.guidegenie.guide.Guide;
import io.hz.guidegenie.guide.GuideRevision;
import io.hz.guidegenie.guide.GuideStatus;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;

public final class GuideDtos {

    private GuideDtos() {
    }

    public record CreateRequest(
        @NotBlank String title,
        String contentMd
    ) {
    }

    public record UpdateRequest(
        @NotBlank String title,
        @NotBlank String contentMd
    ) {
    }

    public record GenerateRequest(
        @NotBlank String prompt
    ) {
    }

    /** 비동기 생성 작업 접수 응답. */
    public record GenerateAccepted(
        String jobId
    ) {
    }

    public record Response(
        Long id,
        Long projectId,
        String title,
        GuideStatus status,
        String createdBy
    ) {
        public static Response from(Guide g) {
            return new Response(g.getId(), g.getProjectId(), g.getTitle(), g.getStatus(),
                g.getCreatedBy());
        }
    }

    public record RevisionResponse(
        Long id,
        int version,
        String contentMd,
        String editedBy,
        OffsetDateTime createdAt
    ) {
        public static RevisionResponse from(GuideRevision r) {
            return new RevisionResponse(r.getId(), r.getVersion(), r.getContentMd(),
                r.getEditedBy(), r.getCreatedAt());
        }
    }
}
