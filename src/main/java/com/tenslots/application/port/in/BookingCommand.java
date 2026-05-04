package com.tenslots.application.port.in;

import com.tenslots.domain.payment.InvalidPaymentCombinationException;
import com.tenslots.domain.payment.PaymentMethod;
import com.tenslots.global.api.code.common.ErrorCode;
import com.tenslots.global.exception.BusinessException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record BookingCommand(
        String productId,
        Long userId,
        String idempotencyKey,
        List<PaymentDetail> payments
) {
    public static BookingCommand of(
            String productId,
            Long userId,
            String idempotencyKey,
            List<PaymentDetail> payments
    ) {
        Set<PaymentMethod> methods = payments.stream()
                .map(PaymentDetail::method)
                .collect(Collectors.toSet());

        // 동일 수단 중복 입력 → pgIdempotencyKey 충돌로 이중 청구 위험
        if (methods.size() != payments.size()) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_COMBINATION);
        }

        try {
            PaymentMethod.validateCombination(methods);
        } catch (InvalidPaymentCombinationException e) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_COMBINATION);
        }

        return new BookingCommand(productId, userId, idempotencyKey, payments);
    }

    public record PaymentDetail(PaymentMethod method, BigDecimal amount) {}
}
