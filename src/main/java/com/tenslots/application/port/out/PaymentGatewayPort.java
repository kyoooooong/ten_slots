package com.tenslots.application.port.out;

import com.tenslots.domain.payment.PaymentMethod;

import java.math.BigDecimal;

public interface PaymentGatewayPort {

    PaymentResult process(PaymentRequest request);

    PaymentResult inquiry(String pgIdempotencyKey);

    // 복합 결제 중 일부 실패 시 기승인 건 취소 — 실서비스에서는 PG사 취소 API 호출
    void cancel(String pgTransactionId);

    PaymentMethod supportedMethod();

    record PaymentRequest(String pgIdempotencyKey, PaymentMethod method, BigDecimal amount) {}

    record PaymentResult(boolean success, String pgTransactionId, String errorMessage) {}
}
