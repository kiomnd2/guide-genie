package io.hz.guidegenie.guide;

import io.hz.guidegenie.common.NotFoundException;
import io.hz.guidegenie.embedding.EmbeddingService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GuideService {

    private final GuideRepository guideRepository;
    private final GuideRevisionRepository revisionRepository;
    private final EmbeddingService embeddingService;

    /** 수동 생성 또는 자동 생성 초안 저장. 초기 리비전(v1)을 함께 만든다. */
    @Transactional
    public Guide createDraft(Long projectId, String title, String contentMd, String author,
                             Map<String, Object> generationMeta) {
        Guide guide = guideRepository.save(new Guide(projectId, title, author, generationMeta));
        saveRevision(guide, contentMd == null ? "" : contentMd, author);
        return guide;
    }

    @Transactional(readOnly = true)
    public List<Guide> findByProject(Long projectId) {
        return guideRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public Guide get(Long guideId) {
        return guideRepository.findById(guideId)
            .orElseThrow(() -> new NotFoundException("Guide not found: " + guideId));
    }

    /** 제목/본문 수정 → 새 리비전 생성 및 재임베딩. */
    @Transactional
    public Guide update(Long guideId, String title, String contentMd, String editor) {
        Guide guide = get(guideId);
        guide.rename(title);
        saveRevision(guide, contentMd, editor);
        return guide;
    }

    @Transactional
    public Guide publish(Long guideId) {
        Guide guide = get(guideId);
        guide.publish();
        // 게시본을 Q&A 검색 인덱스에 반영
        latestRevision(guideId)
            .ifPresent(r -> embeddingService.embedGuide(guideId, guide.getTitle(), r.getContentMd()));
        return guide;
    }

    @Transactional(readOnly = true)
    public List<GuideRevision> revisions(Long guideId) {
        return revisionRepository.findByGuideIdOrderByVersionDesc(guideId);
    }

    private void saveRevision(Guide guide, String contentMd, String editor) {
        int nextVersion = latestRevision(guide.getId())
            .map(GuideRevision::getVersion)
            .orElse(0) + 1;
        revisionRepository.save(new GuideRevision(guide.getId(), contentMd, nextVersion, editor));
        // 수정 시 재임베딩(게시 상태에 관계없이 최신 초안 반영은 정책에 따라 조정)
        embeddingService.embedGuide(guide.getId(), guide.getTitle(), contentMd);
    }

    private java.util.Optional<GuideRevision> latestRevision(Long guideId) {
        return revisionRepository.findFirstByGuideIdOrderByVersionDesc(guideId);
    }
}
