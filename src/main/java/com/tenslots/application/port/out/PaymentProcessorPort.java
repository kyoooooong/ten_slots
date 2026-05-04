package com.tenslots.application.port.out;

import com.tenslots.domain.payment.PaymentMethod;

public interface PaymentProcessorPort {

    PaymentGatewayPort.PaymentResult process(PaymentMethod method, PaymentGatewayPort.PaymentRequest request);

    PaymentGatewayPort.PaymentResult inquiry(PaymentMethod method, String pgIdempotencyKey);

    void cancel(PaymentMethod method, String pgTransactionId);
}
