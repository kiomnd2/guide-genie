package io.hz.guidegenie.auth;

import io.hz.guidegenie.common.TokenCipher;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.stereotype.Component;

/**
 * DB(sso_provider) 기반 동적 {@link ClientRegistrationRepository}.
 *
 * <p>정적 {@code application.yml} 등록을 대체한다. registrationId로 조회 시 DB에서 provider를 읽어
 * issuer의 OIDC discovery(.well-known/openid-configuration)로 {@link ClientRegistration}을 구성하며,
 * discovery 비용을 줄이기 위해 결과를 캐시한다. provider 변경 시 {@link #evictAll()}로 무효화한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicClientRegistrationRepository implements ClientRegistrationRepository {

    private final SsoProviderRepository providerRepository;
    private final TokenCipher tokenCipher;

    private final ConcurrentHashMap<String, ClientRegistration> cache = new ConcurrentHashMap<>();

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        ClientRegistration cached = cache.get(registrationId);
        if (cached != null) {
            return cached;
        }
        return providerRepository.findByRegistrationIdAndEnabledTrue(registrationId)
            .map(this::build)
            .map(reg -> {
                cache.put(registrationId, reg);
                return reg;
            })
            .orElse(null);
    }

    private ClientRegistration build(SsoProvider p) {
        Set<String> scopes = Arrays.stream(p.getScopes().split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());

        // issuer discovery로 authorization/token/jwk endpoint를 자동 구성
        return ClientRegistrations.fromIssuerLocation(p.getIssuerUri())
            .registrationId(p.getRegistrationId())
            .clientId(p.getClientId())
            .clientSecret(tokenCipher.decrypt(p.getEncryptedClientSecret()))
            .scope(scopes)
            .clientName(p.getDisplayName() != null ? p.getDisplayName() : p.getRegistrationId())
            .build();
    }

    public void evict(String registrationId) {
        cache.remove(registrationId);
    }

    public void evictAll() {
        cache.clear();
    }
}
