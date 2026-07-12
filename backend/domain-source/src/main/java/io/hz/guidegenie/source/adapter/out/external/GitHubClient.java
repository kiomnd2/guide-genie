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
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * GitHub 커넥터 — REST API로 저장소의 README와 파일 구조를 수집한다.
 * config: {owner, repo, branch?}. 토큰(PAT)은 암호화 저장된 값을 복호화해 사용.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubClient implements SourceConnectorPort {

    private static final String API = "https://api.github.com";
    private static final int MAX_TREE_ENTRIES = 500;

    private final TokenCipher tokenCipher;

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

        // 1) README
        try {
            JsonNode readme = client.get().uri("/repos/{o}/{r}/readme", owner, repo)
                .retrieve().body(JsonNode.class);
            if (readme != null && readme.hasNonNull("content")) {
                String content = new String(
                    Base64.getMimeDecoder().decode(readme.get("content").asText()),
                    StandardCharsets.UTF_8);
                docs.add(new RawDocument("readme", owner + "/" + repo + " README",
                    content, readme.path("html_url").asText(null)));
            }
        } catch (Exception e) {
            log.warn("[GitHub] README 수집 실패: {}", e.toString());
        }

        // 2) 파일 구조(재귀 트리) → 구조 설명 문서
        try {
            JsonNode tree = client.get()
                .uri("/repos/{o}/{r}/git/trees/{b}?recursive=1", owner, repo, branch)
                .retrieve().body(JsonNode.class);
            JsonNode entries = tree == null ? null : tree.get("tree");
            if (entries != null && entries.isArray()) {
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
                docs.add(new RawDocument("__tree__", owner + "/" + repo + " 파일 구조",
                    sb.toString(), "https://github.com/" + owner + "/" + repo));
            }
        } catch (Exception e) {
            log.warn("[GitHub] 트리 수집 실패: {}", e.toString());
        }

        log.info("[GitHub] fetchAll {}/{}@{} → {} docs", owner, repo, branch, docs.size());
        return docs;
    }

    @Override
    public List<RawDocument> fetchSince(SourceConnection connection, OffsetDateTime since) {
        // 단순화: 증분 대신 전체 재수집(멱등 upsert). 추후 commits since 기반으로 개선.
        return fetchAll(connection);
    }

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
}
