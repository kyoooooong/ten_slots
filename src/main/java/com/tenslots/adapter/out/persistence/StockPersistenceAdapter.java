package com.tenslots.adapter.out.persistence;

import com.tenslots.application.port.out.LoadStockPort;
import com.tenslots.domain.stock.Stock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StockPersistenceAdapter implements LoadStockPort {

    private final StockJpaRepository stockJpaRepository;

    @Override
    public Optional<Stock> findByProductId(String productId) {
        return stockJpaRepository.findById(productId);
    }
}
