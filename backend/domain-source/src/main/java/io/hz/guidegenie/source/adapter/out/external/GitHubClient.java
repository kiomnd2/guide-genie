package io.hz.guidegenie.source.adapter.out.external;

import com.fasterxml.jackson.databind.JsonNode;
import io.hz.guidegenie.common.TokenCipher;
import io.hz.guidegenie.source.application.port.out.RawDocument;
import io.hz.guidegenie.source.application.port.out.SourceConnectorPort;
import io.hz.guidegenie.source.domain.SourceConnection;
import io.hz.guidegenie.source.domain.SourceType;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * GitHub 커넥터 — REST API로 저장소를 읽어 RAG 색인용 문서를 수집한다.
 * <ol>
 *   <li>README 본문</li>
 *   <li>파일 구조(재귀 트리) → 구조 설명 문서</li>
 *   <li>의미 있는 파일 본문(문서 &gt; 설정 &gt; 소스 순, 개수·크기 제한) — 상세 가이드 생성의 근거</li>
 * </ol>
 * config: {owner, repo, branch?}. 토큰(PAT)은 암호화 저장된 값을 복호화해 사용.
 */
@Slf4j
@Component
public class GitHubClient implements SourceConnectorPort {

    private static final String API = "https://api.github.com";
    private static final int MAX_TREE_ENTRIES = 500;

    /** 하위 경로 어디든 포함되면 수집 대상에서 제외하는 디렉토리. */
    private static final Set<String> SKIP_DIRS = Set.of(
        "node_modules", ".git", "build", "dist", "target", "out", "bin", "vendor",
        ".gradle", ".idea", ".vscode", "coverage", "__pycache__", ".next", ".venv", "venv");

    /** 우선순위 0 — 문서. */
    private static final Set<String> DOC_EXT = Set.of("md", "markdown", "mdx", "rst", "adoc", "txt");

    /** 우선순위 1 — 설정/매니페스트(파일명 기준). */
    private static final Set<String> CONFIG_NAMES = Set.of(
        "dockerfile", "docker-compose.yml", "docker-compose.yaml", "makefile", "package.json",
        "pom.xml", "build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts",
        "requirements.txt", "pyproject.toml", "go.mod", "cargo.toml", ".env.example");

    /** 우선순위 2 — 소스/설정 확장자. */
    private static final Set<String> SOURCE_EXT = Set.of(
        "java", "kt", "kts", "gradle", "ts", "tsx", "js", "jsx", "py", "go", "rb", "rs",
        "c", "cc", "cpp", "h", "hpp", "cs", "php", "swift", "scala", "sql", "sh",
        "yaml", "yml", "toml", "xml", "properties", "gql", "graphql", "proto");

    private final TokenCipher tokenCipher;

    /** 본문 수집 최대 파일 수(트리에서 우선순위·깊이 순으로 선별). */
    @Value("${guidegenie.github.max-files:40}")
    private int maxFiles;

    /** 파일당 최대 바이트 — 초과 파일은 색인에서 제외(대용량/바이너리 방지). */
    @Value("${guidegenie.github.max-file-bytes:100000}")
    private long maxFileBytes;

    public GitHubClient(TokenCipher tokenCipher) {
        this.tokenCipher = tokenCipher;
    }

    @Override
    public boolean supports(SourceType type) {
        return type == SourceType.GITHUB;
    }

    @Override
    public boolean testConnection(SourceConnection connection) {
        try {
            client(connection).get().uri("/repos/{o}/{r}", owner(connection), repo(connection))
                .retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.warn("[GitHub] testConnection 실패: {}", e.toString());
            return false;
        }
    }

    @Override
    public List<RawDocument> fetchAll(SourceConnection connection) {
        RestClient client = client(connection);
        String owner = owner(connection);
        String repo = repo(connection);
        String branch = branch(connection);
        List<RawDocument> docs = new ArrayList<>();

        fetchReadme(client, owner, repo).ifPresent(docs::add);

        JsonNode entries = fetchTreeEntries(client, owner, repo, branch);
        if (entries != null && entries.isArray()) {
            docs.add(buildStructureDoc(owner, repo, branch, entries));
            docs.addAll(fetchFiles(client, owner, repo, branch, entries));
        }

        log.info("[GitHub] fetchAll {}/{}@{} → {} docs", owner, repo, branch, docs.size());
        return docs;
    }

    @Override
    public List<RawDocument> fetchSince(SourceConnection connection, OffsetDateTime since) {
        // 단순화: 증분 대신 전체 재수집(멱등 upsert). 추후 commits since 기반으로 개선.
        return fetchAll(connection);
    }

    // ---- 수집 단계 ----

    private Optional<RawDocument> fetchReadme(RestClient client, String owner, String repo) {
        try {
            JsonNode readme = client.get().uri("/repos/{o}/{r}/readme", owner, repo)
                .retrieve().body(JsonNode.class);
            if (readme != null && readme.hasNonNull("content")) {
                String content = decodeBase64(readme.get("content").asText());
                return Optional.of(new RawDocument("readme", owner + "/" + repo + " README",
                    content, readme.path("html_url").asText(null)));
            }
        } catch (Exception e) {
            log.warn("[GitHub] README 수집 실패: {}", e.toString());
        }
        return Optional.empty();
    }

    private JsonNode fetchTreeEntries(RestClient client, String owner, String repo, String branch) {
        try {
            JsonNode tree = client.get()
                .uri("/repos/{o}/{r}/git/trees/{b}?recursive=1", owner, repo, branch)
                .retrieve().body(JsonNode.class);
            return tree == null ? null : tree.get("tree");
        } catch (Exception e) {
            log.warn("[GitHub] 트리 수집 실패: {}", e.toString());
            return null;
        }
    }

    private RawDocument buildStructureDoc(String owner, String repo, String branch, JsonNode entries) {
        StringBuilder sb = new StringBuilder(
            "저장소 " + owner + "/" + repo + " (" + branch + ") 파일 구조:\n");
        int n = 0;
        for (JsonNode node : entries) {
            if (!"blob".equals(node.path("type").asText())) {
                continue;
            }
            sb.append("- ").append(node.path("path").asText()).append('\n');
            if (++n >= MAX_TREE_ENTRIES) {
                sb.append("... (이하 생략)\n");
                break;
            }
        }
        return new RawDocument("__tree__", owner + "/" + repo + " 파일 구조",
            sb.toString(), "https://github.com/" + owner + "/" + repo);
    }

    /** 트리에서 대상 파일을 우선순위·깊이 순으로 선별해 본문(blob)을 수집한다. */
    private List<RawDocument> fetchFiles(RestClient client, String owner, String repo,
                                         String branch, JsonNode entries) {
        List<Candidate> candidates = new ArrayList<>();
        for (JsonNode node : entries) {
            if (!"blob".equals(node.path("type").asText())) {
                continue;
            }
            String path = node.path("path").asText();
            long size = node.path("size").asLong(0);
            int priority = priority(path);
            if (priority < 0 || isSkipped(path) || isReadme(path) || size <= 0 || size > maxFileBytes) {
                continue;
            }
            candidates.add(new Candidate(path, node.path("sha").asText(), priority, depth(path)));
        }
        candidates.sort(Comparator.comparingInt((Candidate c) -> c.priority)
            .thenComparingInt(c -> c.depth)
            .thenComparing(c -> c.path));

        List<RawDocument> files = new ArrayList<>();
        for (Candidate c : candidates) {
            if (files.size() >= maxFiles) {
                break;
            }
            try {
                JsonNode blob = client.get().uri("/repos/{o}/{r}/git/blobs/{sha}", owner, repo, c.sha)
                    .retrieve().body(JsonNode.class);
                if (blob == null || !"base64".equals(blob.path("encoding").asText())) {
                    continue;
                }
                String content = decodeBase64(blob.path("content").asText());
                if (content.isBlank()) {
                    continue;
                }
                files.add(new RawDocument(
                    "file:" + c.path,
                    owner + "/" + repo + " :: " + c.path,
                    "파일 " + c.path + "\n\n" + content, // 경로를 본문에 포함 → 청크가 출처 맥락을 갖는다
                    "https://github.com/" + owner + "/" + repo + "/blob/" + branch + "/" + c.path));
            } catch (Exception e) {
                log.warn("[GitHub] 파일 수집 실패 {}: {}", c.path, e.toString());
            }
        }
        log.info("[GitHub] fetchFiles {}/{} → {}개 수집(후보 {}개, 상한 {})",
            owner, repo, files.size(), candidates.size(), maxFiles);
        return files;
    }

    // ---- 선별 규칙 ----

    private static int priority(String path) {
        String name = fileName(path).toLowerCase(Locale.ROOT);
        if (CONFIG_NAMES.contains(name)) {
            return 1;
        }
        String ext = ext(name);
        if (DOC_EXT.contains(ext)) {
            return 0;
        }
        if (SOURCE_EXT.contains(ext)) {
            return 2;
        }
        return -1;
    }

    private static boolean isSkipped(String path) {
        for (String seg : path.split("/")) {
            if (SKIP_DIRS.contains(seg.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isReadme(String path) {
        return fileName(path).toLowerCase(Locale.ROOT).startsWith("readme");
    }

    private static int depth(String path) {
        return (int) path.chars().filter(ch -> ch == '/').count();
    }

    private static String fileName(String path) {
        int i = path.lastIndexOf('/');
        return i < 0 ? path : path.substring(i + 1);
    }

    private static String ext(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i + 1);
    }

    private static String decodeBase64(String base64) {
        return new String(Base64.getMimeDecoder().decode(base64), StandardCharsets.UTF_8);
    }

    // ---- 연결/설정 ----

    private RestClient client(SourceConnection connection) {
        String token = tokenCipher.decrypt(connection.getEncryptedToken());
        RestClient.Builder b = RestClient.builder().baseUrl(API)
            .defaultHeader("Accept", "application/vnd.github+json")
            .defaultHeader("X-GitHub-Api-Version", "2022-11-28");
        if (token != null && !token.isBlank()) {
            b.defaultHeader("Authorization", "Bearer " + token);
        }
        return b.build();
    }

    private String owner(SourceConnection c) {
        return cfg(c, "owner");
    }

    private String repo(SourceConnection c) {
        return cfg(c, "repo");
    }

    private String branch(SourceConnection c) {
        String b = cfg(c, "branch");
        return b.isBlank() ? "main" : b;
    }

    private String cfg(SourceConnection c, String key) {
        Object v = c.getConfig() == null ? null : c.getConfig().get(key);
        return v == null ? "" : v.toString().trim();
    }

    /** 본문 수집 후보 — 선별·정렬용. */
    private record Candidate(String path, String sha, int priority, int depth) {
    }
}
