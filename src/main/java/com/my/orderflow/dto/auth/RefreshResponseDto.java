package com.my.orderflow.dto.auth;

public record RefreshResponseDto(

         String accessToken,

         String tokenType,

         Long expiresIn
) {
}
