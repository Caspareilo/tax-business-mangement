package com.example.tax.exception;

public class BusinessException extends AppException {

    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super("BUSINESS_ERROR", message);
    }
}
