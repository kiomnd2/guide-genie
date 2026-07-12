package io.hz.guidegenie.guide.application.service;

import io.hz.guidegenie.common.NotFoundException;
import io.hz.guidegenie.guide.application.port.out.GuideRepositoryPort;
import io.hz.guidegenie.guide.application.port.out.GuideRevisionRepositoryPort;
import io.hz.guidegenie.guide.domain.Guide;
import io.hz.guidegenie.guide.domain.GuideRevision;
import io.hz.guidegenie.rag.application.port.in.IndexPort;
import io.hz.guidegenie.rag.domain.RefType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가이드 유스케이스. 저장 시마다 리비전 생성, 게시/수정 시 RAG 색인(IndexPort)에 반영.
 */
@Service
@RequiredArgsConstructor
public class GuideService {

    private final GuideRepositoryPort guideRepository;
    private final GuideRevisionRepositoryPort revisionRepository;
    private final IndexPort ragIndex; // domain-rag 인바운드 포트

    @Transactional
    public Guide createDraft(Long projectId, String title, Long categoryId,
                             String contentMd, String author, Map<String, Object> generationMeta) {
        Guide guide = guideRepository.save(new Guide(projectId, title, categoryId, author, generationMeta));
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

    @Transactional
    public Guide update(Long guideId, String title, Long categoryId, String contentMd, String editor) {
        Guide guide = get(guideId);
        guide.edit(title, categoryId);
        saveRevision(guide, contentMd, editor);
        return guide;
    }

    /** 분류만 이동(드래그 앤 드롭). 리비전을 만들지 않는다. */
    @Transactional
    public Guide moveToCategory(Long guideId, Long categoryId) {
        Guide guide = get(guideId);
        guide.moveToCategory(categoryId);
        return guide;
    }

    @Transactional
    public Guide publish(Long guideId) {
        Guide guide = get(guideId);
        guide.publish();
        latestRevision(guideId).ifPresent(r ->
            ragIndex.index(guide.getProjectId(), RefType.GUIDE, guideId, guide.getTitle(), r.getContentMd()));
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
        ragIndex.index(guide.getProjectId(), RefType.GUIDE, guide.getId(), guide.getTitle(), contentMd);
    }

    private Optional<GuideRevision> latestRevision(Long guideId) {
        return revisionRepository.findLatest(guideId);
    }
}
