package com.tenslots.adapter.out.persistence;

import com.tenslots.domain.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface ProductJpaRepository extends JpaRepository<Product, String> {

    List<Product> findByOpenAtBetween(OffsetDateTime from, OffsetDateTime to);
}
