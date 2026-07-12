package io.hz.guidegenie.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/** 현재 인증 주체(사내 SSO JWT)의 식별자 유틸. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** JWT 클레임에서 사용자 식별자(email 우선, 없으면 sub)를 반환. */
    public static String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return "anonymous";
        }
        if (auth.getPrincipal() instanceof Jwt jwt) {
            String email = jwt.getClaimAsString("email");
            return email != null ? email : jwt.getSubject();
        }
        return auth.getName();
    }
}
