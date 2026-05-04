package com.tenslots.adapter.out.persistence;

import com.tenslots.domain.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentJpaRepository extends JpaRepository<Payment, String> {}
