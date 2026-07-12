package io.hz.guidegenie.api.guide;

import io.hz.guidegenie.guide.application.service.GuideService;
import io.hz.guidegenie.guide.domain.GuideRevision;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guides/{guideId}/revisions")
@RequiredArgsConstructor
public class GuideRevisionController {

    private final GuideService guideService;

    @GetMapping
    public List<Response> list(@PathVariable Long guideId) {
        return guideService.revisions(guideId).stream().map(Response::from).toList();
    }

    public record Response(Long id, int version, String contentMd, String editedBy,
                           OffsetDateTime createdAt) {
        static Response from(GuideRevision r) {
            return new Response(r.getId(), r.getVersion(), r.getContentMd(), r.getEditedBy(),
                r.getCreatedAt());
        }
    }
}
