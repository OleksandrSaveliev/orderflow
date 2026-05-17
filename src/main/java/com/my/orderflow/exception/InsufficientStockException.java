package com.my.orderflow.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String productTitle, int requested, int available) {
        super(String.format("Insufficient stock for '%s': requested %d, available %d",
                productTitle, requested, available));
    }
}