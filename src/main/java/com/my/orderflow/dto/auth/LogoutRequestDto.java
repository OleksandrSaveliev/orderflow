package com.my.orderflow.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequestDto(
        @NotBlank String refreshToken
) {}