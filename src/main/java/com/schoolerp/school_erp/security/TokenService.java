package com.schoolerp.school_erp.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TokenService {

    private final SecretKeySpec secretKeySpec;

    public TokenService(@Value("${school.auth.token.secret:default-secret-key-for-school-erp}") String secret) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(secret.getBytes(StandardCharsets.UTF_8));
            byte[] key16Bytes = Arrays.copyOf(keyBytes, 16); // 128 bit AES key
            this.secretKeySpec = new SecretKeySpec(key16Bytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize TokenService encryption key", e);
        }
    }

    public String encryptRoles(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return encryptString("");
        }
        String rolesStr = String.join(",", roles);
        return encryptString(rolesStr);
    }

    public Set<String> decryptRoles(String encryptedRoles) {
        if (encryptedRoles == null || encryptedRoles.isEmpty()) {
            return Set.of();
        }
        String rolesStr = decryptString(encryptedRoles);
        if (rolesStr.isEmpty()) {
            return Set.of();
        }
        return Arrays.stream(rolesStr.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    public String generateToken(UUID userId, Set<String> roles) {
        try {
            String encryptedRoles = encryptRoles(roles);
            String rawToken = userId.toString() + ":" + encryptedRoles;
            return Base64.getUrlEncoder().encodeToString(rawToken.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate token", e);
        }
    }

    public AuthToken parseToken(String tokenStr) {
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(tokenStr);
            String rawToken = new String(decodedBytes, StandardCharsets.UTF_8);
            int colonIndex = rawToken.indexOf(':');
            if (colonIndex == -1) {
                throw new IllegalArgumentException("Invalid token format");
            }
            UUID userId = UUID.fromString(rawToken.substring(0, colonIndex));
            String encryptedRoles = rawToken.substring(colonIndex + 1);
            return new AuthToken(userId, encryptedRoles);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid token format", e);
        }
    }

    private String encryptString(String value) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    private String decryptString(String encrypted) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] decrypted = cipher.doFinal(Base64.getUrlDecoder().decode(encrypted));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
