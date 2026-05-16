package com.my.orderflow.dto.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResponseDto(

        UUID id,

        String title,

        String description,

        BigDecimal price,

        Integer quantity,

        UUID categoryId,

        LocalDateTime createdAt,

        LocalDateTime updatedAt
) {
}