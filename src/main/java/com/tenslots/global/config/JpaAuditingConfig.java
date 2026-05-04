package com.tenslots.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "offsetDateTimeProvider")
public class JpaAuditingConfig {

    // OffsetDateTime 필드(@CreatedDate/@LastModifiedDate)에 LocalDateTime이 주입되면 타입 불일치 오류 발생
    // → DateTimeProvider를 명시적으로 등록해 OffsetDateTime을 직접 반환
    @Bean
    public DateTimeProvider offsetDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now(ZoneOffset.UTC));
    }

    // 인증 도입 전 임시값 — 추후 SecurityContextHolder에서 사용자 ID 반환으로 교체
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of("system");
    }
}
