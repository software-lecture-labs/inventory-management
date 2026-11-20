package com.inventory.exception;

import org.springframework.http.HttpStatus;

/**
 * İş kuralı ihlallerinde fırlatılır (ör. çakışan rezervasyon, tekrar eden seri no).
 * Varsayılan HTTP durumu 400'dür.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
