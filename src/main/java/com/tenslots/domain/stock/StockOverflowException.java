package com.tenslots.domain.stock;

// 재고 복구 시 최대치 초과 — 정상 플로우에서는 발생하지 않아야 하는 프로그래밍 오류
public class StockOverflowException extends RuntimeException {

    public StockOverflowException(String productId, int current, int max) {
        super("재고 상한 초과 - productId: " + productId + ", current: " + current + ", max: " + max);
    }
}
