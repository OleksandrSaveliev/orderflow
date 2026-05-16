package com.my.orderflow.exception;

public class ProductAlreadyExistsException extends RuntimeException {
    public ProductAlreadyExistsException(String title) {
        super("Product already exists with title: " + title);
    }
}