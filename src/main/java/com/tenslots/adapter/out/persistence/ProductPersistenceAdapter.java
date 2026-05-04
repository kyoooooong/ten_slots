package com.tenslots.adapter.out.persistence;

import com.tenslots.application.port.out.LoadProductPort;
import com.tenslots.domain.product.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductPersistenceAdapter implements LoadProductPort {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public Optional<Product> findById(String productId) {
        return productJpaRepository.findById(productId);
    }

    @Override
    public List<Product> findScheduledBetween(OffsetDateTime from, OffsetDateTime to) {
        return productJpaRepository.findByOpenAtBetween(from, to);
    }
}
