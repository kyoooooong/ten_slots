package com.tenslots.adapter.out.redis;

import com.tenslots.adapter.out.persistence.DbStockAdapter;
import com.tenslots.application.port.out.StockPort;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Primary
@Component
public class CircuitBreakerStockAdapter implements StockPort {

    private static final String CB_NAME = "stockDecrease";

    private final RedisStockAdapter redisStockAdapter;
    private final DbStockAdapter dbStockAdapter;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    // Redis 장애 → DB fallback 발동 횟수 — CB OPEN 여부와 함께 장애 감지 지표
    private final Counter redisFallbackCounter;

    public CircuitBreakerStockAdapter(
            RedisStockAdapter redisStockAdapter,
            DbStockAdapter dbStockAdapter,
            CircuitBreakerRegistry circuitBreakerRegistry,
            MeterRegistry meterRegistry
    ) {
        this.redisStockAdapter = redisStockAdapter;
        this.dbStockAdapter = dbStockAdapter;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.redisFallbackCounter = Counter.builder("stock.redis.fallback")
                .description("Redis 장애로 DB fallback이 발동된 횟수")
                .register(meterRegistry);
    }

    @Override
    public boolean decrease(String productId) {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(CB_NAME);
        long start = System.nanoTime();

        // CB OPEN → Redis 시도 없이 즉시 DB로 전환
        try {
            cb.acquirePermission();
        } catch (CallNotPermittedException e) {
            log.warn("Circuit breaker OPEN, fallback to DB - productId: {}", productId);
            redisFallbackCounter.increment();
            return dbStockAdapter.decrease(productId);
        }

        try {
            boolean result = redisStockAdapter.decrease(productId);
            cb.onSuccess(System.nanoTime() - start, TimeUnit.NANOSECONDS);
            return result;
        } catch (StockKeyNotFoundException e) {
            // warm-up 미실행으로 키 없음 — Redis 장애가 아니므로 CB failure 미집계
            cb.onSuccess(System.nanoTime() - start, TimeUnit.NANOSECONDS);
            log.info("Stock key not found in Redis, using DB fallback - productId: {}", productId);
            return dbStockAdapter.decrease(productId);
        } catch (Exception e) {
            cb.onError(System.nanoTime() - start, TimeUnit.NANOSECONDS, e);
            log.warn("Redis unavailable, fallback to DB - productId: {}, reason: {}", productId, e.getMessage());
            redisFallbackCounter.increment();
            return dbStockAdapter.decrease(productId);
        }
    }

    @Override
    public void increase(String productId) {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(CB_NAME);
        long start = System.nanoTime();

        try {
            cb.acquirePermission();
        } catch (CallNotPermittedException e) {
            log.warn("Circuit breaker OPEN, fallback to DB increase - productId: {}", productId);
            dbStockAdapter.increase(productId);
            return;
        }

        try {
            redisStockAdapter.increase(productId);
            cb.onSuccess(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        } catch (StockKeyNotFoundException e) {
            // decrease()도 DB fallback했다는 의미 — CB failure 미집계, DB로 복구
            cb.onSuccess(System.nanoTime() - start, TimeUnit.NANOSECONDS);
            log.info("Stock key not found in Redis, using DB fallback for increase - productId: {}", productId);
            dbStockAdapter.increase(productId);
        } catch (Exception e) {
            cb.onError(System.nanoTime() - start, TimeUnit.NANOSECONDS, e);
            log.warn("Redis unavailable, fallback to DB increase - productId: {}, reason: {}", productId, e.getMessage());
            dbStockAdapter.increase(productId);
        }
    }

    @Override
    public void init(String productId, int quantity) {
        // warm-up은 best-effort — Redis 장애 시 해당 상품만 건너뜀
        try {
            redisStockAdapter.init(productId, quantity);
        } catch (Exception e) {
            log.warn("Redis warm-up skipped - productId: {}, reason: {}", productId, e.getMessage());
        }
    }
}
