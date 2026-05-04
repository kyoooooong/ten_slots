package com.tenslots.application.service;

import com.tenslots.application.port.in.BookingCommand;
import com.tenslots.application.port.out.PaymentGatewayPort;
import com.tenslots.application.port.out.PaymentProcessorPort;
import com.tenslots.application.port.out.SavePaymentPort;
import com.tenslots.domain.payment.Payment;
import com.tenslots.domain.payment.PaymentStatus;
import com.tenslots.global.api.code.common.ErrorCode;
import com.tenslots.global.exception.BusinessException;
import com.tenslots.global.exception.PaymentTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompositePaymentProcessor {

    private final PaymentProcessorPort paymentProcessorPort;
    private final SavePaymentPort savePaymentPort;

    public List<Payment> process(String bookingId, List<BookingCommand.PaymentDetail> paymentDetails) {
        List<Payment> approved = new ArrayList<>();
        try {
            for (BookingCommand.PaymentDetail detail : paymentDetails) {
                // PG 멱등키: bookingId + 결제수단 — 재시도 시 이중 청구 방지
                String pgIdempotencyKey = bookingId + ":" + detail.method().name().toLowerCase();

                // REQUIRES_NEW 즉시 커밋 — 서버 재시작 후 pgIdempotencyKey로 PG 상태 조회 가능
                Payment payment = savePaymentPort.save(Payment.builder()
                        .bookingId(bookingId)
                        .method(detail.method())
                        .amount(detail.amount())
                        .status(PaymentStatus.REQUESTED)
                        .pgIdempotencyKey(pgIdempotencyKey)
                        .build());

                PaymentGatewayPort.PaymentResult result = callPg(detail, pgIdempotencyKey);

                if (!result.success()) {
                    payment.fail();
                    savePaymentPort.save(payment);
                    // 복합 결제 중 하나라도 실패 → 앞서 승인된 결제 전부 취소
                    cancelApproved(approved);
                    throw new BusinessException(ErrorCode.PAYMENT_FAILED);
                }

                // REQUIRES_NEW 즉시 커밋 — 외부 TX 롤백과 무관하게 PG 승인 기록 보존
                payment.approve(result.pgTransactionId());
                savePaymentPort.save(payment);
                approved.add(payment);
            }
            return approved;
        } catch (Exception e) {
            if (e instanceof BusinessException be) throw be;
            log.error("Unexpected error during payment processing - bookingId: {}", bookingId, e);
            cancelApproved(approved);
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }
    }

    private PaymentGatewayPort.PaymentResult callPg(BookingCommand.PaymentDetail detail, String pgIdempotencyKey) {
        try {
            return paymentProcessorPort.process(
                    detail.method(),
                    new PaymentGatewayPort.PaymentRequest(pgIdempotencyKey, detail.method(), detail.amount())
            );
        } catch (PaymentTimeoutException e) {
            // timeout → 동일 키로 조회해 실제 처리 여부 확인 (이중 청구 방지)
            log.warn("PG timeout, inquiring payment status - pgIdempotencyKey: {}", pgIdempotencyKey);
            return paymentProcessorPort.inquiry(detail.method(), pgIdempotencyKey);
        }
    }

    private void cancelApproved(List<Payment> approved) {
        approved.forEach(payment -> {
            try {
                paymentProcessorPort.cancel(payment.getMethod(), payment.getPgTransactionId());
            } catch (Exception e) {
                // PG 취소 실패 → DB는 FAILED 기록, 수동 대사 처리 필요
                log.error("PG cancel failed - pgTransactionId: {}, method: {}",
                        payment.getPgTransactionId(), payment.getMethod(), e);
            }
            try {
                payment.fail();
                savePaymentPort.save(payment);
            } catch (Exception e) {
                log.error("Failed to mark payment as FAILED - paymentId: {}", payment.getId(), e);
            }
        });
    }
}
