package com.example.tax.util;

import com.example.tax.dto.Page;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

public final class WebUtil {

    public static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private WebUtil() {
    }

    public static String trim(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }

    public static int intParam(HttpServletRequest request, String name, int defaultValue) {
        String value = trim(request, name);
        if (value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static long longParam(HttpServletRequest request, String name, long defaultValue) {
        String value = trim(request, name);
        if (value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static Long optionalLong(HttpServletRequest request, String name) {
        long value = longParam(request, name, -1L);
        return value > 0 ? value : null;
    }

    /** 1-based page number, clamped to a sane range. */
    public static int pageParam(HttpServletRequest request) {
        int page = intParam(request, "page", 1);
        return Math.max(page, 1);
    }

    public static int pageSizeParam(HttpServletRequest request) {
        int size = intParam(request, "size", DEFAULT_PAGE_SIZE);
        if (size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    public static int offset(int page, int size) {
        return (Math.max(page, 1) - 1) * size;
    }

    /** Keeps page labels sane when a filter change reduces the total page count. */
    public static int clampPage(int page, int totalPages) {
        if (totalPages <= 0) {
            return 1;
        }
        return Math.min(Math.max(page, 1), totalPages);
    }

    public static Page<?> emptyPage(int page, int size) {
        return new Page<>(java.util.Collections.emptyList(), page, size, 0);
    }

    public static void flashSuccess(HttpServletRequest request, String message) {
        request.getSession().setAttribute("flashSuccess", message);
    }

    public static void flashError(HttpServletRequest request, String message) {
        request.getSession().setAttribute("flashError", message);
    }

    public static void redirect(HttpServletRequest request, HttpServletResponse response, String path)
            throws IOException {
        response.sendRedirect(request.getContextPath() + path);
    }

    public static void redirectWithSuccess(HttpServletRequest request, HttpServletResponse response,
                                           String path, String message) throws IOException {
        flashSuccess(request, message);
        redirect(request, response, path);
    }

    public static void redirectWithError(HttpServletRequest request, HttpServletResponse response,
                                         String path, String message) throws IOException {
        flashError(request, message);
        redirect(request, response, path);
    }

    /**
     * Rebuilds the current filter query string for pagination links, dropping page/size.
     * Values are URL-encoded; only whitelisted parameter names are preserved.
     */
    public static String filterQuery(HttpServletRequest request, String... names) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String name : names) {
            String value = request.getParameter(name);
            if (value != null && !value.trim().isEmpty()) {
                values.put(name, value.trim());
            }
        }
        return buildQuery(values);
    }

    public static String buildQuery(Map<String, String> values) {
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (query.length() > 0) {
                query.append('&');
            }
            query.append(urlEncode(entry.getKey())).append('=').append(urlEncode(entry.getValue()));
        }
        return query.toString();
    }

    public static String currentQuery(HttpServletRequest request) {
        String query = request.getQueryString();
        return query == null ? "" : query;
    }

    public static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void exposeCsrfToken(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        request.setAttribute("csrfToken", com.example.tax.security.CsrfUtil.getToken(session));
    }
}
