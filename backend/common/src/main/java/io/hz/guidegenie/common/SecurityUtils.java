package io.hz.guidegenie.common;

/**
 * 현재 사용자 식별 유틸. 인증(SSO/JWT)은 아직 미도입 — 감사 필드에 기본 사용자 값을 채운다.
 * 인증 도입 시 실제 주체를 반환하도록 교체한다.
 */
public final class SecurityUtils {

    private static final String DEFAULT_USER = "anonymous";

    private SecurityUtils() {
    }

    public static String currentUser() {
        return DEFAULT_USER;
    }
}
