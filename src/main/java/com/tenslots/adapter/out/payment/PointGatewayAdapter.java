package com.tenslots.adapter.out.payment;

import com.tenslots.application.port.out.PaymentGatewayPort;
import com.tenslots.domain.payment.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j

@Component
public class PointGatewayAdapter implements PaymentGatewayPort {

    @Override
    public PaymentResult process(PaymentRequest request) {
        return new PaymentResult(true, "mock-point-" + request.pgIdempotencyKey(), null);
    }

    @Override
    public PaymentResult inquiry(String pgIdempotencyKey) {
        return new PaymentResult(true, "mock-point-" + pgIdempotencyKey, null);
    }

    @Override
    public void cancel(String pgTransactionId) {
        // Mock: 실서비스에서는 포인트 복원 처리
        log.info("Point cancel - pgTransactionId: {}", pgTransactionId);
    }

    @Override
    public PaymentMethod supportedMethod() {
        return PaymentMethod.POINT;
    }
}
