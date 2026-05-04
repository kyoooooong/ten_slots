package com.tenslots.adapter.out.persistence;

import com.tenslots.application.port.out.ConfirmBookingPort;
import com.tenslots.domain.booking.Booking;
import com.tenslots.domain.payment.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ConfirmBookingPersistenceAdapter implements ConfirmBookingPort {

    private final BookingJpaRepository bookingJpaRepository;
    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    @Transactional
    public Booking confirm(Booking booking, List<Payment> payments) {
        // booking CONFIRMED + payment CONFIRMED를 단일 TX로 — 상태 불일치 방지
        Booking confirmed = bookingJpaRepository.save(booking);
        paymentJpaRepository.saveAll(payments);
        return confirmed;
    }
}
