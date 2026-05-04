package com.tenslots.application.port.out;

public interface IdempotencyPort {

    boolean tryAcquire(String key);

    void release(String key);
}
