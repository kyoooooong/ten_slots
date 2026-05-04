package com.tenslots.adapter.out.persistence;

import com.tenslots.domain.stock.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockJpaRepository extends JpaRepository<Stock, String> {}
