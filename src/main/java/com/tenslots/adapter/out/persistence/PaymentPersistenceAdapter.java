package com.tenslots.adapter.out.persistence;

import com.tenslots.application.port.out.SavePaymentPort;
import com.tenslots.domain.payment.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PaymentPersistenceAdapter implements SavePaymentPort {

    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment save(Payment payment) {
        // REQUIRES_NEW — 외부 TX 롤백과 무관하게 결제 상태 즉시 커밋 (PG 승인 기록 소실 방지)
        return paymentJpaRepository.save(payment);
    }
}
