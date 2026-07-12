package io.hz.guidegenie.config;

import io.hz.guidegenie.auth.DynamicJwtAuthenticationManagerResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 사내 SSO(OIDC) 로그인 → JWT 자원서버 검증 구조.
 *
 * <p>SSO provider는 {@code application.yml}에 고정하지 않고 DB(sso_provider)에서 동적으로 로드한다.
 * API 인증은 토큰의 issuer별로 검증기를 선택하는 다중 issuer 리졸버를 사용한다.
 */
@Configuration
public class SecurityConfig {

    /**
     * 다중 issuer 리졸버. 토큰의 {@code iss}를 추출해
     * {@link DynamicJwtAuthenticationManagerResolver}(DB 신뢰 목록 기반)에 위임한다.
     */
    @Bean
    public AuthenticationManagerResolver<HttpServletRequest> jwtAuthenticationManagerResolver(
            DynamicJwtAuthenticationManagerResolver dynamicResolver) {
        return new JwtIssuerAuthenticationManagerResolver(dynamicResolver);
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            AuthenticationManagerResolver<HttpServletRequest> jwtAuthenticationManagerResolver)
            throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/actuator/health",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            // JWT 자원서버: issuer별 동적 검증
            .oauth2ResourceServer(oauth ->
                oauth.authenticationManagerResolver(jwtAuthenticationManagerResolver));

        return http.build();
    }
}
