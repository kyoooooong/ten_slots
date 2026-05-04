package com.tenslots.application.port.in;

public interface CheckoutUseCase {

    CheckoutResponse checkout(String productId, Long userId);
}
