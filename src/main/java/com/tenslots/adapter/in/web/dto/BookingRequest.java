package com.tenslots.adapter.in.web.dto;

import com.tenslots.application.port.in.BookingCommand;
import com.tenslots.domain.payment.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record BookingRequest(
        @NotBlank(message = "productId는 필수입니다.")
        String productId,

        @NotNull(message = "userId는 필수입니다.")
        Long userId,

        @NotBlank(message = "idempotencyKey는 필수입니다.")
        String idempotencyKey,

        @NotEmpty(message = "payments는 최소 1개 이상이어야 합니다.")
        @Valid
        List<PaymentDetailRequest> payments
) {
    public record PaymentDetailRequest(
            @NotNull(message = "결제 수단은 필수입니다.")
            PaymentMethod method,

            @NotNull(message = "결제 금액은 필수입니다.")
            @Positive(message = "결제 금액은 0보다 커야 합니다.")
            BigDecimal amount
    ) {
        public BookingCommand.PaymentDetail toCommand() {
            return new BookingCommand.PaymentDetail(method, amount);
        }
    }

    public BookingCommand toCommand() {
        // 생성자 직접 호출 대신 정적 팩토리 사용 — 비즈니스 검증(결제 수단 조합)이 내부에서 처리됨
        return BookingCommand.of(
                productId,
                userId,
                idempotencyKey,
                payments.stream().map(PaymentDetailRequest::toCommand).toList()
        );
    }
}
