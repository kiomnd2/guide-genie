package io.hz.guidegenie.api.template;

import io.hz.guidegenie.guide.domain.TemplateItem;
import io.hz.guidegenie.source.domain.SourceConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 연동 정보 기준 템플릿 항목 자동 생성 프리셋.
 * 소스 타입별로 추천 가이드 항목(제목 + 프롬프트)을 config 값(저장소/프로젝트키/스페이스키)에 맞춰 채운다.
 * (app 조합 계층: domain-source 읽기 → domain-guide 템플릿 항목 구성)
 */
final class ConnectionTemplatePresets {

    private ConnectionTemplatePresets() {
    }

    static List<TemplateItem> build(List<SourceConnection> connections) {
        List<TemplateItem> items = new ArrayList<>();
        for (SourceConnection c : connections) {
            Map<String, Object> cfg = c.getConfig() == null ? Map.of() : c.getConfig();
            switch (c.getType()) {
                case GITHUB -> {
                    String repo = trimSlash(str(cfg, "owner") + "/" + str(cfg, "repo"));
                    String ref = repo.isBlank() ? "이 GitHub 저장소" : repo + " 저장소";
                    items.add(item("로컬 개발환경 세팅",
                        ref + "를 로컬에서 세팅하는 방법(클론·의존성 설치·실행)을 신규 입사자용으로 정리해줘"));
                    items.add(item("프로젝트 구조 개요",
                        ref + "의 디렉토리·모듈 구조와 주요 컴포넌트를 설명해줘"));
                    items.add(item("빌드·배포 절차",
                        ref + "의 빌드·테스트·배포 절차를 정리해줘"));
                    items.add(item("코드 기여 가이드",
                        ref + "의 브랜치 전략·PR/코드리뷰 규칙·커밋 컨벤션을 정리해줘"));
                }
                case JIRA -> {
                    String pk = str(cfg, "projectKey");
                    String ref = pk.isBlank() ? "이 Jira 프로젝트" : "Jira 프로젝트 " + pk;
                    items.add(item("프로젝트 개요", ref + "의 목적과 범위를 정리해줘"));
                    items.add(item("이슈 워크플로우",
                        ref + "의 이슈 유형과 상태 전이(워크플로우)를 설명해줘"));
                }
                case CONFLUENCE -> {
                    String sk = str(cfg, "spaceKey");
                    String ref = sk.isBlank() ? "이 Confluence 스페이스" : "Confluence 스페이스 " + sk;
                    items.add(item("문서 공간 안내",
                        ref + "의 주요 문서와 찾는 방법을 안내해줘"));
                }
                default -> { /* 알 수 없는 타입은 건너뜀 */ }
            }
        }
        return items;
    }

    private static TemplateItem item(String title, String prompt) {
        return new TemplateItem(title, prompt, null); // 분류는 사용자가 편집에서 지정
    }

    private static String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? "" : v.toString().trim();
    }

    private static String trimSlash(String s) {
        return s.replaceAll("^/+|/+$", "");
    }
}
