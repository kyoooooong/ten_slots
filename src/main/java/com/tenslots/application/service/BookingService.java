package com.tenslots.application.service;

import com.tenslots.application.port.in.BookingCommand;
import com.tenslots.application.port.in.BookingResponse;
import com.tenslots.application.port.in.BookingUseCase;
import com.tenslots.application.port.out.ConfirmBookingPort;
import com.tenslots.application.port.out.IdempotencyPort;
import com.tenslots.application.port.out.LoadProductPort;
import com.tenslots.application.port.out.SaveBookingPort;
import com.tenslots.domain.booking.Booking;
import com.tenslots.domain.booking.BookingStatus;
import com.tenslots.domain.payment.Payment;
import com.tenslots.domain.product.Product;
import com.tenslots.application.port.out.StockPort;
import com.tenslots.global.api.code.common.ErrorCode;
import com.tenslots.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService implements BookingUseCase {

    private final StockPort stockPort;
    private final IdempotencyPort idempotencyPort;
    private final LoadProductPort loadProductPort;
    private final SaveBookingPort saveBookingPort;
    private final ConfirmBookingPort confirmBookingPort;
    private final CompositePaymentProcessor compositePaymentProcessor;
    private final Clock clock;

    @Override
    public BookingResponse book(BookingCommand command) {
        // 1. 중복 요청 차단 (Redis SET NX)
        if (!idempotencyPort.tryAcquire(command.idempotencyKey())) {
            log.warn("Duplicate request detected - idempotencyKey: {}", command.idempotencyKey());
            throw new BusinessException(ErrorCode.DUPLICATE_REQUEST);
        }

        // 2. 상품 조회 + 오픈 여부 + 총액 검증
        //    검증 실패 시 멱등키 해제해 클라이언트 재시도 허용
        try {
            Product product = loadProductPort.findById(command.productId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
            if (OffsetDateTime.now(clock).isBefore(product.getOpenAt())) {
                throw new BusinessException(ErrorCode.BOOKING_NOT_OPEN_YET);
            }
            BigDecimal totalAmount = command.payments().stream()
                    .map(BookingCommand.PaymentDetail::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalAmount.compareTo(product.getPrice()) != 0) {
                throw new BusinessException(ErrorCode.REQUEST_CONTENT_INVALID);
            }
        } catch (BusinessException e) {
            idempotencyPort.release(command.idempotencyKey());
            throw e;
        }

        // 3. 재고 차감 (Redis Lua → CB → DB Fallback)
        try {
            if (!stockPort.decrease(command.productId())) {
                throw new BusinessException(ErrorCode.STOCK_SOLD_OUT);
            }
        } catch (BusinessException e) {
            idempotencyPort.release(command.idempotencyKey());
            throw e;
        }

        // 4. PENDING 예약 생성 — REQUIRES_NEW 즉시 커밋, 서버 장애 시 복구 기준점
        //    저장 실패 시 재고·멱등키 즉시 복구 (재고 유실 방지)
        Booking booking;
        try {
            booking = saveBookingPort.save(Booking.builder()
                    .userId(command.userId())
                    .productId(command.productId())
                    .status(BookingStatus.PENDING)
                    .build());
        } catch (Exception e) {
            log.error("Failed to save PENDING booking, restoring stock - productId: {}", command.productId(), e);
            restoreStock(command.productId());
            idempotencyPort.release(command.idempotencyKey());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        log.info("Booking created as PENDING - bookingId: {}, productId: {}", booking.getId(), command.productId());

        // 5. PG 결제 (트랜잭션 밖 — 커넥션 점유 없이 PG 응답 대기)
        List<Payment> approvedPayments;
        try {
            approvedPayments = compositePaymentProcessor.process(booking.getId(), command.payments());
        } catch (BusinessException e) {
            log.warn("Payment failed, rolling back - bookingId: {}", booking.getId());
            failBooking(booking);
            restoreStock(command.productId());
            idempotencyPort.release(command.idempotencyKey());
            throw e;
        }

        // 6. 예약 + 결제 확정 — 단일 TX로 원자적 처리 (불일치 방지)
        booking.confirm();
        approvedPayments.forEach(Payment::confirm);
        Booking confirmed = confirmBookingPort.confirm(booking, approvedPayments);

        log.info("Booking confirmed - bookingId: {}", confirmed.getId());
        return BookingResponse.of(confirmed, approvedPayments);
    }

    private void failBooking(Booking booking) {
        try {
            booking.fail();
            saveBookingPort.save(booking);
        } catch (Exception e) {
            // 저장 실패 시 PENDING 유지 — pgIdempotencyKey로 수동 복구
            log.error("Failed to mark booking as FAILED - bookingId: {}", booking.getId(), e);
        }
    }

    private void restoreStock(String productId) {
        try {
            stockPort.increase(productId);
        } catch (Exception e) {
            // 복구 실패 시 warm-up 재실행 또는 수동 복구 필요
            log.error("Failed to restore stock - productId: {}", productId, e);
        }
    }
}
