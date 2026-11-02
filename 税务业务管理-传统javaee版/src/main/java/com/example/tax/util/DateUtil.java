package com.example.tax.util;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class DateUtil {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private DateUtil() {
    }

    public static LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static String formatDate(LocalDate value) {
        return value == null ? "" : DATE_FORMAT.format(value);
    }

    public static String formatDateTime(LocalDateTime value) {
        return value == null ? "" : DATETIME_FORMAT.format(value);
    }

    public static String formatTimestamp(Timestamp timestamp) {
        return timestamp == null ? "" : DATETIME_FORMAT.format(timestamp.toLocalDateTime());
    }

    public static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
