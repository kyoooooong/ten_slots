package com.tenslots.adapter.out.redis;

import com.tenslots.application.port.out.StockPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisStockAdapter implements StockPort {

    private static final String KEY_PREFIX = "stock:";
    private static final long STOCK_TTL_HOURS = 24;

    // EXISTS → DECR → 소진 검증을 원자적으로 처리
    // -2: 키 없음, -1: 재고 소진, 0 이상: 성공
    private static final DefaultRedisScript<Long> DECREASE_SCRIPT;

    static {
        DECREASE_SCRIPT = new DefaultRedisScript<>();
        DECREASE_SCRIPT.setScriptText(
                "if redis.call('EXISTS', KEYS[1]) == 0 then\n" +
                "    return -2\n" +
                "end\n" +
                "local current = redis.call('DECR', KEYS[1])\n" +
                "if current < 0 then\n" +
                "    redis.call('INCR', KEYS[1])\n" +
                "    return -1\n" +
                "end\n" +
                "return current"
        );
        DECREASE_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean decrease(String productId) {
        Long result = stringRedisTemplate.execute(
                DECREASE_SCRIPT,
                List.of(KEY_PREFIX + productId)
        );
        if (result == null || result == -2L) {
            throw new StockKeyNotFoundException(productId);
        }
        return result >= 0;
    }

    @Override
    public void increase(String productId) {
        // 키가 없으면 decrease()도 DB fallback했다는 의미 — orphan key 생성 방지
        String key = KEY_PREFIX + productId;
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            throw new StockKeyNotFoundException(productId);
        }
        stringRedisTemplate.opsForValue().increment(key);
    }

    @Override
    public void init(String productId, int quantity) {
        // setIfAbsent — 판매 중 스케줄러 재실행 시 진행 중인 재고 덮어쓰지 않음
        String key = KEY_PREFIX + productId;
        stringRedisTemplate.opsForValue().setIfAbsent(key, String.valueOf(quantity), STOCK_TTL_HOURS, TimeUnit.HOURS);
    }
}
