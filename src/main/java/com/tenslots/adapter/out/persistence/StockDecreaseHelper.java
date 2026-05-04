package com.tenslots.adapter.out.persistence;

import com.tenslots.domain.stock.Stock;
import com.tenslots.global.api.code.common.ErrorCode;
import com.tenslots.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class StockDecreaseHelper {

    private final StockJpaRepository stockJpaRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean decrease(String productId) {
        // REQUIRES_NEW — 매 시도마다 새 트랜잭션, @Version 충돌 시 ObjectOptimisticLockingFailureException
        Stock stock = stockJpaRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return stock.decrease();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void increase(String productId) {
        Stock stock = stockJpaRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        stock.increase();
    }
}
