package com.my.orderflow.dto.auth;

public record AuthResponseDto(

        String accessToken,

        String refreshToken,

        String tokenType,

        Long expiresIn
) {
}
