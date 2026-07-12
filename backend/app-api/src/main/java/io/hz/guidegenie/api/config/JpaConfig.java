package io.hz.guidegenie.api.config;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** JPA 감사 활성화 — 모든 domain-* 엔티티의 @CreatedDate/@LastModifiedDate 채움. */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaConfig {

    /** 기본 provider는 LocalDateTime을 반환해 OffsetDateTime 필드 변환에 실패 → 직접 공급. */
    @Bean
    public DateTimeProvider auditingDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }
}
