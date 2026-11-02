package com.example.tax.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class CsrfUtil {

    public static final String SESSION_KEY = "csrfToken";
    public static final String PARAM_NAME = "csrfToken";
    private static final SecureRandom RANDOM = new SecureRandom();

    private CsrfUtil() {
    }

    public static String getToken(HttpSession session) {
        if (session == null) {
            return "";
        }
        Object existing = session.getAttribute(SESSION_KEY);
        if (existing instanceof String && !((String) existing).isEmpty()) {
            return (String) existing;
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(SESSION_KEY, token);
        return token;
    }

    public static boolean isValid(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        Object expected = session.getAttribute(SESSION_KEY);
        String actual = request.getParameter(PARAM_NAME);
        if (!(expected instanceof String) || actual == null) {
            return false;
        }
        return constantTimeEquals((String) expected, actual);
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        byte[] a = expected.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] b = actual.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}
