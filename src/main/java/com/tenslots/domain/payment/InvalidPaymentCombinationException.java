package com.tenslots.domain.payment;

// 허용되지 않는 결제 수단 조합 — 도메인 규칙 위반
public class InvalidPaymentCombinationException extends RuntimeException {

    public InvalidPaymentCombinationException(String message) {
        super(message);
    }
}
