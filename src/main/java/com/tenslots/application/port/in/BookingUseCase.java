package com.tenslots.application.port.in;

public interface BookingUseCase {

    BookingResponse book(BookingCommand command);
}
