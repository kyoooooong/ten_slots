package com.tenslots.application.port.out;

public interface StockPort {

    boolean decrease(String productId);

    void increase(String productId);

    void init(String productId, int quantity);
}
