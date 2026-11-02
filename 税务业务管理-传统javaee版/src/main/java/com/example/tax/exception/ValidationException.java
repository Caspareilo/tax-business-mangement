package com.example.tax.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ValidationException extends AppException {

    private static final long serialVersionUID = 1L;

    private final Map<String, String> fieldErrors;

    public ValidationException(String message) {
        this(message, Collections.emptyMap());
    }

    public ValidationException(String message, Map<String, String> fieldErrors) {
        super("VALIDATION_ERROR", message);
        this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
