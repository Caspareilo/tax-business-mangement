package com.example.tax.exception;

public class ForbiddenException extends AppException {

    private static final long serialVersionUID = 1L;

    public ForbiddenException(String message) {
        super("FORBIDDEN", message);
    }
}
