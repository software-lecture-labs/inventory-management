package com.inventory.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Tüm hata cevaplarının ortak formatı.
 * fieldErrors sadece doğrulama hatalarında doldurulur.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String message,
        Map<String, String> fieldErrors
) {
    public static ApiError of(int status, String message) {
        return new ApiError(LocalDateTime.now(), status, message, null);
    }
}
