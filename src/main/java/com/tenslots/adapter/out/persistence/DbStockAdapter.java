package com.tenslots.adapter.out.persistence;

import com.tenslots.application.port.out.StockPort;
import com.tenslots.global.api.code.common.ErrorCode;
import com.tenslots.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DbStockAdapter implements StockPort {

    private static final int MAX_RETRY = 3;

    private final StockDecreaseHelper stockDecreaseHelper;

    @Override
    public boolean decrease(String productId) {
        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            try {
                return stockDecreaseHelper.decrease(productId);
            } catch (ObjectOptimisticLockingFailureException e) {
                log.warn("Optimistic lock conflict, retry {}/{} - productId: {}", attempt + 1, MAX_RETRY, productId);
                if (attempt == MAX_RETRY - 1) {
                    log.error("Optimistic lock retry exhausted - productId: {}", productId);
                    throw new BusinessException(ErrorCode.LOCK_CONFLICT);
                }
            }
        }
        return false;
    }

    @Override
    public void increase(String productId) {
        // 실패(StockOverflowException 포함)는 그대로 전파 — 호출부에서 로그 처리
        stockDecreaseHelper.increase(productId);
    }

    @Override
    public void init(String productId, int quantity) {
        throw new UnsupportedOperationException();
    }
}
