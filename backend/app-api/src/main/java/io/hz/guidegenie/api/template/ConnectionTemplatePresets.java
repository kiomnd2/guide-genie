package io.hz.guidegenie.api.template;

import io.hz.guidegenie.guide.domain.DetailLevel;
import io.hz.guidegenie.guide.domain.TemplateItem;
import io.hz.guidegenie.source.domain.SourceConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 연동 정보 기준 템플릿 항목 자동 생성 프리셋.
 * 소스 타입별로 추천 가이드 항목(제목·프롬프트·대상 독자·목차·상세 수준)을
 * config 값(저장소/프로젝트키/스페이스키)에 맞춰 채운다.
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
                        ref + "를 로컬에서 세팅하는 방법(클론·의존성 설치·실행)을 신규 입사자용으로 정리해줘",
                        "신규 입사 개발자",
                        List.of("사전 준비물", "저장소 클론", "의존성 설치", "환경 변수·설정",
                            "실행 및 동작 확인", "자주 겪는 문제(트러블슈팅)"),
                        DetailLevel.DETAILED));
                    items.add(item("프로젝트 구조 개요",
                        ref + "의 디렉토리·모듈 구조와 주요 컴포넌트를 설명해줘",
                        "프로젝트에 처음 합류한 개발자",
                        List.of("전체 구조 한눈에 보기", "주요 디렉토리·모듈", "핵심 컴포넌트와 책임",
                            "모듈 간 의존 관계"),
                        DetailLevel.STANDARD));
                    items.add(item("빌드·배포 절차",
                        ref + "의 빌드·테스트·배포 절차를 정리해줘",
                        "개발자 및 배포 담당자",
                        List.of("전제조건", "빌드 방법", "테스트 실행", "배포 단계", "롤백·주의사항"),
                        DetailLevel.DETAILED));
                    items.add(item("코드 기여 가이드",
                        ref + "의 브랜치 전략·PR/코드리뷰 규칙·커밋 컨벤션을 정리해줘",
                        "기여하려는 개발자",
                        List.of("브랜치 전략", "커밋 컨벤션", "PR 작성 규칙", "코드리뷰 절차"),
                        DetailLevel.STANDARD));
                }
                case JIRA -> {
                    String pk = str(cfg, "projectKey");
                    String ref = pk.isBlank() ? "이 Jira 프로젝트" : "Jira 프로젝트 " + pk;
                    items.add(item("프로젝트 개요",
                        ref + "의 목적과 범위를 정리해줘",
                        "신규 참여자",
                        List.of("프로젝트 목적", "범위와 주요 이해관계자", "핵심 일정·마일스톤"),
                        DetailLevel.STANDARD));
                    items.add(item("이슈 워크플로우",
                        ref + "의 이슈 유형과 상태 전이(워크플로우)를 설명해줘",
                        "이슈를 다루는 담당자",
                        List.of("이슈 유형", "상태 전이(워크플로우)", "담당자·우선순위 규칙",
                            "라벨·컴포넌트 사용법"),
                        DetailLevel.DETAILED));
                }
                case CONFLUENCE -> {
                    String sk = str(cfg, "spaceKey");
                    String ref = sk.isBlank() ? "이 Confluence 스페이스" : "Confluence 스페이스 " + sk;
                    items.add(item("문서 공간 안내",
                        ref + "의 주요 문서와 찾는 방법을 안내해줘",
                        "문서를 찾는 팀원",
                        List.of("스페이스 구성", "주요 문서 위치", "문서 검색·탐색 방법",
                            "문서 작성·기여 규칙"),
                        DetailLevel.STANDARD));
                }
                default -> { /* 알 수 없는 타입은 건너뜀 */ }
            }
        }
        return items;
    }

    private static TemplateItem item(String title, String prompt, String audience,
                                     List<String> sections, DetailLevel detailLevel) {
        return new TemplateItem(title, prompt, null, audience, sections, detailLevel); // 분류는 사용자가 편집에서 지정
    }

    private static String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? "" : v.toString().trim();
    }

    private static String trimSlash(String s) {
        return s.replaceAll("^/+|/+$", "");
    }
}
