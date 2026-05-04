package com.tenslots.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    // 서버는 UTC로 운영 — OffsetDateTime으로 절대 시각을 저장하고 UTC 기준으로 비교
    // Clock을 빈으로 분리해 테스트 시 Clock.fixed()로 대체 가능
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
