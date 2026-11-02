package com.example.tax.security;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {

    private static final int WORK_FACTOR = 10;

    private PasswordUtil() {
    }

    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(WORK_FACTOR));
    }

    public static boolean matches(String plainPassword, String hash) {
        if (plainPassword == null || hash == null || hash.trim().isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static String generateTemporaryPassword() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder builder = new StringBuilder(12);
        for (int i = 0; i < 10; i++) {
            builder.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        builder.append("#7");
        return builder.toString();
    }

    public static boolean isStrongEnough(String password) {
        if (password == null || password.length() < 8 || password.length() > 64) {
            return false;
        }
        boolean letter = false;
        boolean digit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                letter = true;
            } else if (Character.isDigit(c)) {
                digit = true;
            }
        }
        return letter && digit;
    }
}
