package com.my.orderflow.dto.cart;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponseDto(

        UUID id,

        List<CartItemResponseDto> items,

        BigDecimal totalAmount
) {
}