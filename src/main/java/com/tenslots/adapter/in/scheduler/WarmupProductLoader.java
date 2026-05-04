package com.tenslots.adapter.in.scheduler;

import com.tenslots.application.port.out.LoadProductPort;
import com.tenslots.domain.product.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * StockWarmupScheduler의 DB 조회 전용 빈.
 * self-call은 Spring AOP 프록시를 우회해 @Transactional이 무시되므로 별도 빈으로 분리.
 */
@Component
@RequiredArgsConstructor
public class WarmupProductLoader {

    private static final int WARMUP_WINDOW_MINUTES = 10;

    private final LoadProductPort loadProductPort;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<Product> loadScheduledProducts() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        return loadProductPort.findScheduledBetween(now, now.plusMinutes(WARMUP_WINDOW_MINUTES));
    }
}
