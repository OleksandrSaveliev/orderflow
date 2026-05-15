package com.my.orderflow.dto.auth;

public record RefreshTokenResponseDto(

         String accessToken,

         String tokenType,

         Long expiresIn
) {
}
