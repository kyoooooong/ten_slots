package com.tenslots.adapter.out.redis;

import com.tenslots.application.port.out.IdempotencyPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisIdempotencyAdapter implements IdempotencyPort {

    private static final String KEY_PREFIX = "idempotency:";
    // 성공 예약은 키를 유지해 당일 재예약 차단 (실패 시에만 release로 재시도 허용)
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean tryAcquire(String key) {
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + key, "processing", TTL);
        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public void release(String key) {
        stringRedisTemplate.delete(KEY_PREFIX + key);
    }
}
