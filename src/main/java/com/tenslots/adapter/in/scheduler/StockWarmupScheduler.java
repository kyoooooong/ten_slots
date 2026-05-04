package com.tenslots.adapter.in.scheduler;

import com.tenslots.application.port.out.LoadStockPort;
import com.tenslots.application.port.out.StockPort;
import com.tenslots.domain.product.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockWarmupScheduler {

    // DB 조회를 별도 빈으로 분리 — self-call은 Spring AOP를 우회해 @Transactional이 무시됨
    private final WarmupProductLoader warmupProductLoader;
    private final LoadStockPort loadStockPort;
    private final StockPort stockPort;

    @Scheduled(cron = "${scheduler.warmup.cron}")
    public void warmup() {
        // DB 커넥션을 Redis I/O 대기 동안 점유하지 않도록 DB 조회 완료 후 트랜잭션 종료
        List<Product> products = warmupProductLoader.loadScheduledProducts();

        for (Product product : products) {
            loadStockPort.findByProductId(product.getId()).ifPresentOrElse(
                    stock -> {
                        stockPort.init(product.getId(), stock.getQuantity());
                        log.info("Redis stock initialized - productId: {}, quantity: {}", product.getId(), stock.getQuantity());
                    },
                    () -> log.warn("Stock not found for warmup - productId: {}", product.getId())
            );
        }
    }
}
