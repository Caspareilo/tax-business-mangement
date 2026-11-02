package com.example.tax.util;

import com.example.tax.exception.ValidationException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class ValidationUtil {

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_.-]{3,64}$");
    private static final Pattern CODE = Pattern.compile("^[A-Za-z0-9_-]{2,64}$");

    private ValidationUtil() {
    }

    public static Map<String, String> newErrors() {
        return new LinkedHashMap<>();
    }

    public static void throwIfAny(Map<String, String> errors, String message) {
        if (!errors.isEmpty()) {
            throw new ValidationException(message, errors);
        }
    }

    public static void requireText(Map<String, String> errors, String field, String label,
                                   String value, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            errors.put(field, label + "不能为空");
        } else if (value.trim().length() > maxLength) {
            errors.put(field, label + "长度不能超过 " + maxLength + " 个字符");
        }
    }

    public static void optionalText(Map<String, String> errors, String field, String label,
                                    String value, int maxLength) {
        if (value != null && value.trim().length() > maxLength) {
            errors.put(field, label + "长度不能超过 " + maxLength + " 个字符");
        }
    }

    public static void requireUsername(Map<String, String> errors, String field, String label,
                                       String value) {
        requireText(errors, field, label, value, 64);
        if (!errors.containsKey(field) && !USERNAME.matcher(value.trim()).matches()) {
            errors.put(field, label + "只能包含字母、数字、下划线、点或短横线，长度 3-64");
        }
    }

    public static void requireCode(Map<String, String> errors, String field, String label,
                                   String value) {
        requireText(errors, field, label, value, 64);
        if (!errors.containsKey(field) && !CODE.matcher(value.trim()).matches()) {
            errors.put(field, label + "只能包含字母、数字、下划线或短横线，长度 2-64");
        }
    }

    public static boolean isPositiveDecimal(String value) {
        try {
            return new BigDecimal(value.trim()).compareTo(BigDecimal.ZERO) > 0;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static BigDecimal parseDecimal(String value) {
        try {
            return new BigDecimal(value.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }
}
