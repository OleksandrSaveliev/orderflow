package com.my.orderflow.dto.cart;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponseDto(

        UUID id,

        UUID productId,

        String productTitle,

        Integer quantity,

        BigDecimal priceAtAdd
) {
}