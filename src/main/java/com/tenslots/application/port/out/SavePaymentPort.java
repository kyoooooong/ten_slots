package com.tenslots.application.port.out;

import com.tenslots.domain.payment.Payment;

public interface SavePaymentPort {

    Payment save(Payment payment);
}
