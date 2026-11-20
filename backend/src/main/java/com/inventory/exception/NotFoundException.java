package com.inventory.exception;

/** İstenen kayıt bulunamadığında fırlatılır (HTTP 404). */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
