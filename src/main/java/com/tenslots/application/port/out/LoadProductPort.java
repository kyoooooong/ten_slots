package com.tenslots.application.port.out;

import com.tenslots.domain.product.Product;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface LoadProductPort {

    Optional<Product> findById(String productId);

    List<Product> findScheduledBetween(OffsetDateTime from, OffsetDateTime to);
}
