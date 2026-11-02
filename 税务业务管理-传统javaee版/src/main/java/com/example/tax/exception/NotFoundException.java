package com.example.tax.exception;

public class NotFoundException extends AppException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super("NOT_FOUND", message);
    }
}
