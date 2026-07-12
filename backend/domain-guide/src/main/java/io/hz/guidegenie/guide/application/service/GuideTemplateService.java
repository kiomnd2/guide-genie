package io.hz.guidegenie.guide.application.service;

import io.hz.guidegenie.common.NotFoundException;
import io.hz.guidegenie.guide.application.port.out.GuideTemplateRepositoryPort;
import io.hz.guidegenie.guide.domain.GuideTemplate;
import io.hz.guidegenie.guide.domain.TemplateItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 가이드 세트 템플릿 CRUD + 일괄 생성(run). */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuideTemplateService {

    private final GuideTemplateRepositoryPort repository;
    private final GuideGenerationService generationService;

    @Transactional
    public GuideTemplate create(Long projectId, String name, List<TemplateItem> items, String author) {
        return repository.save(new GuideTemplate(projectId, name, items, author));
    }

    @Transactional(readOnly = true)
    public List<GuideTemplate> findByProject(Long projectId) {
        return repository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public GuideTemplate get(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundException("GuideTemplate not found: " + id));
    }

    @Transactional
    public GuideTemplate update(Long id, String name, List<TemplateItem> items) {
        GuideTemplate template = get(id);
        template.update(name, items);
        return template;
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * 템플릿 일괄 실행 — 항목마다 가이드 초안을 비동기 생성한다.
     * @return 트리거된 항목 수
     */
    public int run(Long id, String author) {
        GuideTemplate template = get(id);
        List<TemplateItem> items = template.getItems();
        for (TemplateItem item : items) {
            generationService.generateOne(
                template.getProjectId(), item.title(), item.categoryId(), item.prompt(), author);
        }
        log.info("[Template] run id={} → {} items triggered", id, items.size());
        return items.size();
    }
}
