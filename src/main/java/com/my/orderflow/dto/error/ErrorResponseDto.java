package com.my.orderflow.dto.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDto(
        int status,
        String error,
        String message,
        String path,
        LocalDateTime timestamp,
        Long retryAfter,
        Map<String, String> validationErrors
) {
    public ErrorResponseDto(int status, String error, String message, String path) {
        this(status, error, message, path, LocalDateTime.now(), null, null);
    }

    public ErrorResponseDto(int status, String error, String message, String path,
                            Map<String, String> validationErrors) {
        this(status, error, message, path, LocalDateTime.now(), null, validationErrors);
    }

    public ErrorResponseDto(int status, String error, String message, String path, Long retryAfter) {
        this(status, error, message, path, LocalDateTime.now(), retryAfter, null);
    }
}
