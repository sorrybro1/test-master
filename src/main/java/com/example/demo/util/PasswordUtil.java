package com.example.demo.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class PasswordUtil {
    private PasswordUtil() {}

    /** MD5(UTF-8) -> UPPERCASE HEX */
    public static String md5Upper(String raw) {
        if (raw == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02X", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 兼容：stored 可能是 hash 或明文
     * - stored == md5(raw) 通过
     * - stored == raw 也通过（用于老数据），通过后你可以“自动升级”成 hash
     */
    public static boolean matchesStored(String stored, String rawInput) {
        if (stored == null || rawInput == null) return false;
        String hash = md5Upper(rawInput);
        return stored.equalsIgnoreCase(hash) || stored.equals(rawInput);
    }
}
