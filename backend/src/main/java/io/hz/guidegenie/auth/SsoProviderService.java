package io.hz.guidegenie.auth;

import io.hz.guidegenie.auth.dto.SsoProviderDtos.CreateRequest;
import io.hz.guidegenie.auth.dto.SsoProviderDtos.UpdateRequest;
import io.hz.guidegenie.common.NotFoundException;
import io.hz.guidegenie.common.TokenCipher;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SsoProviderService {

    private final SsoProviderRepository repository;
    private final TokenCipher tokenCipher;
    private final DynamicClientRegistrationRepository clientRegistrationRepository;
    private final DynamicJwtAuthenticationManagerResolver jwtResolver;

    @Transactional
    public SsoProvider create(CreateRequest req) {
        if (repository.existsByRegistrationId(req.registrationId())) {
            throw new IllegalArgumentException(
                "registrationId already exists: " + req.registrationId());
        }
        SsoProvider provider = new SsoProvider(
            req.registrationId(),
            req.displayName(),
            req.issuerUri(),
            req.clientId(),
            tokenCipher.encrypt(req.clientSecret()),
            req.scopes());
        SsoProvider saved = repository.save(provider);
        evictCaches();
        return saved;
    }

    @Transactional(readOnly = true)
    public List<SsoProvider> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public SsoProvider get(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundException("SsoProvider not found: " + id));
    }

    @Transactional
    public SsoProvider update(Long id, UpdateRequest req) {
        SsoProvider provider = get(id);
        String encrypted = (req.clientSecret() == null || req.clientSecret().isBlank())
            ? null // 미입력 시 기존 secret 유지
            : tokenCipher.encrypt(req.clientSecret());
        provider.update(req.displayName(), req.issuerUri(), req.clientId(), encrypted,
            req.scopes(), req.enabled());
        evictCaches();
        return provider;
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
        evictCaches();
    }

    /** provider 변경 시 로그인/JWT 양쪽 캐시를 무효화한다. */
    private void evictCaches() {
        clientRegistrationRepository.evictAll();
        jwtResolver.evictAll();
    }
}
