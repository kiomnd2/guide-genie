package io.hz.guidegenie.auth;

import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.stereotype.Component;

/**
 * 토큰의 {@code iss}(issuer)별로 검증기를 동적으로 선택하는 리졸버.
 *
 * <p>{@code JwtIssuerAuthenticationManagerResolver}가 토큰에서 issuer를 추출해 이 클래스에 위임한다.
 * DB(sso_provider)의 신뢰 목록에 있는(=enabled) issuer만 허용하며, issuer별
 * {@link AuthenticationManager}는 discovery/JWK 비용을 줄이기 위해 캐시한다.
 *
 * <p><b>보안</b>: DB 화이트리스트에 없는 issuer는 {@code null}을 반환해 인증을 거부한다.
 * 이 검증이 없으면 임의 issuer 토큰을 신뢰하게 되어 위조에 취약하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicJwtAuthenticationManagerResolver
        implements AuthenticationManagerResolver<String> {

    private final SsoProviderRepository providerRepository;

    private final ConcurrentHashMap<String, AuthenticationManager> managers = new ConcurrentHashMap<>();

    @Override
    public AuthenticationManager resolve(String issuer) {
        return managers.computeIfAbsent(issuer, this::createIfTrusted);
    }

    private AuthenticationManager createIfTrusted(String issuer) {
        if (!providerRepository.existsByIssuerUriAndEnabledTrue(issuer)) {
            log.warn("[SSO] rejected untrusted issuer: {}", issuer);
            return null; // 신뢰 목록 밖 → 인증 거부
        }
        // TODO: 필요 시 audience(aud) 검증 Validator 추가
        JwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuer);
        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(decoder);
        log.info("[SSO] trusted issuer registered: {}", issuer);
        return provider::authenticate;
    }

    public void evictAll() {
        managers.clear();
    }
}
