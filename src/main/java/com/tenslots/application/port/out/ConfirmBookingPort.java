package com.tenslots.application.port.out;

import com.tenslots.domain.booking.Booking;
import com.tenslots.domain.payment.Payment;

import java.util.List;

// booking CONFIRMED + payment CONFIRMED를 하나의 트랜잭션으로 묶어 상태 불일치 방지
public interface ConfirmBookingPort {
    Booking confirm(Booking booking, List<Payment> payments);
}
