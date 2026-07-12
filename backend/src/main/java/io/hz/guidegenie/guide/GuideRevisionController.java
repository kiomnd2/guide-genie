package io.hz.guidegenie.guide;

import io.hz.guidegenie.guide.dto.GuideDtos.RevisionResponse;
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
    public List<RevisionResponse> list(@PathVariable Long guideId) {
        return guideService.revisions(guideId).stream()
            .map(RevisionResponse::from)
            .toList();
    }
}
