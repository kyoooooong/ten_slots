package com.tenslots.application.port.in;

import com.tenslots.domain.product.Product;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
public record CheckoutResponse(
        String productId,
        String productName,
        BigDecimal price,
        OffsetDateTime checkInTime,
        OffsetDateTime checkOutTime,
        OffsetDateTime openAt,
        Long userPoint
) {
    public static CheckoutResponse of(Product product, Long userPoint) {
        return new CheckoutResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getCheckInTime(),
                product.getCheckOutTime(),
                product.getOpenAt(),
                userPoint
        );
    }
}
