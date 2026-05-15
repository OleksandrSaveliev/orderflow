package com.my.orderflow.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(

        @NotBlank @Email
        String email,

        @NotBlank @Size(min = 6, max = 100)
        String password,

        @NotBlank
        String firstName,

        @NotBlank
        String lastName
) {
}
