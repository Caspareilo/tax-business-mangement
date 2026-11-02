package com.example.tax.exception;

/**
 * Base unchecked exception for user-facing application failures.
 * Messages are safe to show on pages; stack traces and SQL never are.
 */
public class AppException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;

    public AppException(String code, String message) {
        super(message);
        this.code = code;
    }

    public AppException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
