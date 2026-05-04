package com.tenslots.domain.payment;

import java.util.Set;

public enum PaymentMethod {
    CREDIT_CARD,
    Y_PAY,
    POINT;

    // 신용카드 + Y페이는 같은 결제 네트워크 계열로 혼용 불가
    private static final Set<PaymentMethod> EXCLUSIVE_METHODS = Set.of(CREDIT_CARD, Y_PAY);

    /**
     * 결제 수단 조합 유효성 검증 — CREDIT_CARD + Y_PAY 혼용 금지
     *
     * @throws InvalidPaymentCombinationException 조합이 유효하지 않은 경우
     */
    public static void validateCombination(Set<PaymentMethod> methods) {
        long exclusiveCount = methods.stream()
                .filter(EXCLUSIVE_METHODS::contains)
                .count();
        if (exclusiveCount > 1) {
            throw new InvalidPaymentCombinationException("신용카드와 Y페이는 혼용할 수 없습니다.");
        }
    }
}
