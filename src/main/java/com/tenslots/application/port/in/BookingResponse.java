package com.tenslots.application.port.in;

import com.tenslots.domain.booking.Booking;
import com.tenslots.domain.booking.BookingStatus;
import com.tenslots.domain.payment.Payment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record BookingResponse(
        String bookingId,
        String productId,
        BookingStatus status,
        BigDecimal totalAmount,
        OffsetDateTime confirmedAt
) {
    public static BookingResponse of(Booking booking, List<Payment> payments) {
        BigDecimal totalAmount = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new BookingResponse(
                booking.getId(),
                booking.getProductId(),
                booking.getStatus(),
                totalAmount,
                booking.getConfirmedAt()
        );
    }
}
