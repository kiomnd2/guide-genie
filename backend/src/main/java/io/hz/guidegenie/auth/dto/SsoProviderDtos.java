package io.hz.guidegenie.auth.dto;

import io.hz.guidegenie.auth.SsoProvider;
import jakarta.validation.constraints.NotBlank;

public final class SsoProviderDtos {

    private SsoProviderDtos() {
    }

    public record CreateRequest(
        @NotBlank String registrationId,
        String displayName,
        @NotBlank String issuerUri,
        @NotBlank String clientId,
        String clientSecret,
        String scopes
    ) {
    }

    public record UpdateRequest(
        String displayName,
        @NotBlank String issuerUri,
        @NotBlank String clientId,
        String clientSecret,
        String scopes,
        boolean enabled
    ) {
    }

    /** client secret은 절대 반환하지 않는다. */
    public record Response(
        Long id,
        String registrationId,
        String displayName,
        String issuerUri,
        String clientId,
        String scopes,
        boolean enabled
    ) {
        public static Response from(SsoProvider p) {
            return new Response(p.getId(), p.getRegistrationId(), p.getDisplayName(),
                p.getIssuerUri(), p.getClientId(), p.getScopes(), p.isEnabled());
        }
    }
}
