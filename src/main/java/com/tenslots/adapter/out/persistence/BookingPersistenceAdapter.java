package com.tenslots.adapter.out.persistence;

import com.tenslots.application.port.out.SaveBookingPort;
import com.tenslots.domain.booking.Booking;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BookingPersistenceAdapter implements SaveBookingPort {

    private final BookingJpaRepository bookingJpaRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Booking save(Booking booking) {
        // REQUIRES_NEW — PENDING 저장과 FAILED 저장을 즉시 독립 커밋
        return bookingJpaRepository.save(booking);
    }
}
