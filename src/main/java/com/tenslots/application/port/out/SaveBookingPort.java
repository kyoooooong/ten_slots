package com.tenslots.application.port.out;

import com.tenslots.domain.booking.Booking;

public interface SaveBookingPort {

    Booking save(Booking booking);
}
