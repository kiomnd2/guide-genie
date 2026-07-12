package io.hz.guidegenie.auth;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SsoProviderRepository extends JpaRepository<SsoProvider, Long> {

    Optional<SsoProvider> findByRegistrationIdAndEnabledTrue(String registrationId);

    Optional<SsoProvider> findByRegistrationId(String registrationId);

    boolean existsByIssuerUriAndEnabledTrue(String issuerUri);

    List<SsoProvider> findByEnabledTrue();

    boolean existsByRegistrationId(String registrationId);
}
