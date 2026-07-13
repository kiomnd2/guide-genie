package io.hz.guidegenie.api.template;

import io.hz.guidegenie.guide.domain.DetailLevel;
import io.hz.guidegenie.guide.domain.TemplateItem;
import io.hz.guidegenie.source.domain.SourceConnection;
import io.hz.guidegenie.source.domain.SourceType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GitHub 연동 기준 <b>운영 인수인계 RUNBOOK 세트</b> 프리셋.
 * 실 프로젝트 RUNBOOK(프로젝트개요·시스템구성·소스구조·개발환경·DB·API·배치·배포·외부연계·장애대응)
 * 챕터 구조를 그대로 담아, 각 항목을 상세 목차와 함께 DETAILED로 구성한다.
 * 항목별 프롬프트/목차는 GuideGenerationService의 RUNBOOK 포맷 지침과 결합돼 인수인계 수준 문서를 생성한다.
 */
final class RunbookTemplatePresets {

    private RunbookTemplatePresets() {
    }

    static List<TemplateItem> build(List<SourceConnection> connections) {
        List<SourceConnection> repos = connections.stream()
            .filter(c -> c.getType() == SourceType.GITHUB)
            .toList();
        if (repos.isEmpty()) {
            return List.of();
        }

        List<TemplateItem> items = new ArrayList<>();
        boolean multi = repos.size() > 1;
        for (SourceConnection c : repos) {
            String ref = repoRef(c);
            String suffix = multi ? " (" + ref + ")" : ""; // 저장소 여러 개면 제목으로 구분
            items.addAll(chapters(ref, suffix));
        }
        return items;
    }

    private static List<TemplateItem> chapters(String ref, String suffix) {
        List<TemplateItem> ch = new ArrayList<>();

        ch.add(item("1. 프로젝트 개요" + suffix,
            ref + "의 배경·목적과 시스템이 무엇을 하는지, 핵심 기능과 기술 스택을 인수인계 관점에서 정리해줘",
            "프로젝트를 처음 인수받는 개발자·운영자",
            List.of("프로젝트 배경 및 목적", "핵심 기능 요약", "기술 스택",
                "주요 이해관계자·역할", "관련 문서·자료 위치")));

        ch.add(item("2. 시스템 구성도" + suffix,
            ref + "의 전체 아키텍처와 런타임/인프라 구성을 다이어그램과 함께 설명해줘",
            "시스템 전반을 파악하려는 개발자·운영자",
            List.of("전체 아키텍처 개요(다이어그램)", "주요 컴포넌트와 책임",
                "런타임·인프라 구성", "컴포넌트 간 통신·의존")));

        ch.add(item("3. 소스 코드 구조" + suffix,
            ref + "의 모듈·패키지 레이아웃, 모듈별 책임, 요청→응답 데이터 흐름, 주요 진입점을 정리해줘",
            "코드를 처음 여는 개발자",
            List.of("모듈/디렉토리 구성", "패키지 레이아웃과 모듈별 책임",
                "요청→응답 데이터 흐름(다이어그램)", "주요 진입점(컨트롤러/엔트리포인트)",
                "코드 컨벤션·핵심 규칙")));

        ch.add(item("4. 개발 및 실행 환경" + suffix,
            ref + "를 로컬에서 빌드·실행·테스트하는 방법과 프로파일·환경변수 구성을 정리해줘",
            "로컬 개발 환경을 세팅하는 개발자",
            List.of("사전 준비물", "빌드·실행·테스트 명령", "프로파일별 환경 구성",
                "환경변수·시크릿 관리", "로컬 셋업 절차", "자주 겪는 문제(트러블슈팅)")));

        ch.add(item("5. 데이터베이스 구조" + suffix,
            ref + "의 데이터베이스 종류·전략, 주요 테이블·스키마, 마이그레이션 관리 방식을 정리해줘",
            "데이터 모델을 파악하려는 개발자",
            List.of("DB 종류 및 전략", "주요 테이블·스키마(표)",
                "엔티티/테이블 관계", "마이그레이션 관리", "운영 시 주의점")));

        ch.add(item("6. API 명세 및 운영" + suffix,
            ref + "의 주요 API 그룹, 인증·인가 방식, 응답 규격과 에러 처리를 정리해줘",
            "API를 연동·운영하는 개발자",
            List.of("API 그룹 개요", "인증·인가(권한) 방식", "주요 엔드포인트(표)",
                "공통 응답·에러 규격", "운영 시 주의점")));

        ch.add(item("7. 배치 및 스케줄러" + suffix,
            ref + "의 배치 Job·스케줄러 구성, 트리거 방식, 모니터링·재처리 절차를 정리해줘(없으면 미해당으로 표기)",
            "배치를 운영·모니터링하는 담당자",
            List.of("배치 Job 목록·실행 주기(표)", "스케줄러·트리거 방식",
                "처리 흐름", "모니터링·실패 재처리 절차", "운영 시 주의점")));

        ch.add(item("8. 배포 (CI/CD)" + suffix,
            ref + "의 빌드·배포 파이프라인과 배포 단계, 롤백 절차를 정리해줘",
            "배포를 수행하는 개발자·운영자",
            List.of("배포 파이프라인 개요(다이어그램)", "환경별 배포 흐름",
                "배포 단계·명령", "롤백 절차", "배포 후 검증·트러블슈팅")));

        ch.add(item("9. 외부 연계" + suffix,
            ref + "가 연동하는 외부 시스템·API의 연계 방식과 인증/키 관리, 장애 시 영향을 정리해줘(없으면 미해당으로 표기)",
            "외부 연계를 다루는 개발자·운영자",
            List.of("연계 대상·방식(표)", "인증·키 관리", "연계 데이터 흐름",
                "장애 시 영향·대응")));

        ch.add(item("10. 장애 대응 및 운영 주의사항" + suffix,
            ref + "의 주요 장애 유형과 대응 절차, 로그 확인 방법, 반드시 지켜야 할 운영 주의사항을 정리해줘",
            "운영·장애 대응 담당자",
            List.of("주요 장애 유형·대응 절차(표)", "로그 위치·확인 방법",
                "모니터링 포인트", "반드시 지켜야 할 주의사항")));

        return ch;
    }

    /** RUNBOOK 항목은 상세도가 핵심 — 항상 DETAILED. 분류는 사용자가 편집에서 지정. */
    private static TemplateItem item(String title, String prompt, String audience, List<String> sections) {
        return new TemplateItem(title, prompt, null, audience, sections, DetailLevel.DETAILED);
    }

    private static String repoRef(SourceConnection c) {
        Map<String, Object> cfg = c.getConfig() == null ? Map.of() : c.getConfig();
        String owner = str(cfg, "owner");
        String repo = str(cfg, "repo");
        String full = (owner + "/" + repo).replaceAll("^/+|/+$", "");
        return full.isBlank() ? "이 GitHub 저장소" : full + " 저장소";
    }

    private static String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? "" : v.toString().trim();
    }
}
