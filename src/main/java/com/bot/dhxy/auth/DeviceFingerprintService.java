package com.bot.dhxy.auth;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class DeviceFingerprintService {

    public String getDeviceFingerprint() {
        String source = String.join("|",
                safe(System.getenv("COMPUTERNAME")),
                safe(System.getenv("PROCESSOR_IDENTIFIER")),
                safe(System.getenv("USERNAME")),
                safe(System.getProperty("os.name")),
                safe(System.getProperty("os.arch")),
                safe(System.getProperty("user.home"))
        );

        return sha256(source);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
