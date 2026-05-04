package com.tenslots.global.exception;

public class PaymentTimeoutException extends RuntimeException {

    private final String pgIdempotencyKey;

    public PaymentTimeoutException(String pgIdempotencyKey) {
        super("PG call timed out - pgIdempotencyKey: " + pgIdempotencyKey);
        this.pgIdempotencyKey = pgIdempotencyKey;
    }

    public String getPgIdempotencyKey() {
        return pgIdempotencyKey;
    }
}
