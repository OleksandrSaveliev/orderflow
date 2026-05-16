package com.my.orderflow.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequestDto(

        @NotBlank @Size(max = 255)
        String title,

        String description,

        @NotNull @Positive
        BigDecimal price,

        @NotNull @Positive
        Integer quantity,

        UUID categoryId
) {
}