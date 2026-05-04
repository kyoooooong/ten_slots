package com.tenslots.adapter.out.payment;

import com.tenslots.application.port.out.PaymentGatewayPort;
import com.tenslots.domain.payment.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class YPayGatewayAdapter implements PaymentGatewayPort {

    @Override
    public PaymentResult process(PaymentRequest request) {
        return new PaymentResult(true, "mock-ypay-" + request.pgIdempotencyKey(), null);
    }

    @Override
    public PaymentResult inquiry(String pgIdempotencyKey) {
        return new PaymentResult(true, "mock-ypay-" + pgIdempotencyKey, null);
    }

    @Override
    public void cancel(String pgTransactionId) {
        // Mock: 실서비스에서는 Y페이 취소 API 호출
        log.info("YPay cancel - pgTransactionId: {}", pgTransactionId);
    }

    @Override
    public PaymentMethod supportedMethod() {
        return PaymentMethod.Y_PAY;
    }
}
