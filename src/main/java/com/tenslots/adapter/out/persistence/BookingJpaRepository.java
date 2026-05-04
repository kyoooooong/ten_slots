package com.tenslots.adapter.out.persistence;

import com.tenslots.domain.booking.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingJpaRepository extends JpaRepository<Booking, String> {}
