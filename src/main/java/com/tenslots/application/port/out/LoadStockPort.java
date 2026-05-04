package com.tenslots.application.port.out;

import com.tenslots.domain.stock.Stock;

import java.util.Optional;

public interface LoadStockPort {

    Optional<Stock> findByProductId(String productId);
}
