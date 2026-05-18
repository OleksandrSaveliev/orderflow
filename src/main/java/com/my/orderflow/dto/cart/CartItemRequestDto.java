package com.my.orderflow.dto.cart;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record CartItemRequestDto(

        @NotNull
        UUID productId,

        @NotNull @Positive
        Integer quantity
) {
}