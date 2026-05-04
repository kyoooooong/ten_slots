package com.tenslots.adapter.out.redis;

class StockKeyNotFoundException extends RuntimeException {

    StockKeyNotFoundException(String productId) {
        super("Stock key not found in Redis - productId: " + productId);
    }
}
