package com.example.tax.util;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class CsvUtil {

    private CsvUtil() {
    }

    /**
     * Writes a CSV attachment (UTF-8 BOM so Excel opens Chinese correctly).
     * Cells beginning with = + - @ are prefixed with an apostrophe to reduce
     * spreadsheet formula injection risk.
     */
    public static void write(HttpServletResponse response, String filename,
                             List<String> headers, List<List<String>> rows) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + asciiFileName(filename) + "\"");
        PrintWriter writer = response.getWriter();
        writer.write('﻿');
        writer.write(join(headers));
        writer.write("\r\n");
        for (List<String> row : rows) {
            writer.write(join(row));
            writer.write("\r\n");
        }
        writer.flush();
    }

    private static String join(List<String> cells) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                line.append(',');
            }
            line.append(escape(cells.get(i)));
        }
        return line.toString();
    }

    static String escape(String value) {
        String text = value == null ? "" : value;
        if (!text.isEmpty()) {
            char first = text.charAt(0);
            if (first == '=' || first == '+' || first == '-' || first == '@'
                    || first == '\t' || first == '\r') {
                text = "'" + text;
            }
        }
        boolean mustQuote = text.contains(",") || text.contains("\"")
                || text.contains("\n") || text.contains("\r") || text.startsWith("'");
        String escaped = text.replace("\"", "\"\"");
        return mustQuote ? "\"" + escaped + "\"" : escaped;
    }

    private static String asciiFileName(String filename) {
        String sanitized = filename == null ? "export.csv" : filename.replaceAll("[^A-Za-z0-9._-]", "_");
        return sanitized.toLowerCase(java.util.Locale.ROOT).endsWith(".csv")
                ? sanitized : sanitized + ".csv";
    }

    public static String utf8(String value) {
        return new String(value.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}
