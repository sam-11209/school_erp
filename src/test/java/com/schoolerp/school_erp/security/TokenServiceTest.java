package com.schoolerp.school_erp.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService("my-test-secret-key-that-is-long");
    }

    @Test
    void testTokenGenerationAndParsing() {
        UUID userId = UUID.randomUUID();
        Set<String> roles = Set.of("ADMIN", "TEACHER");

        String tokenStr = tokenService.generateToken(userId, roles);
        assertNotNull(tokenStr);
        assertFalse(tokenStr.isEmpty());

        AuthToken parsedToken = tokenService.parseToken(tokenStr);
        assertEquals(userId, parsedToken.getUserId());
        assertNotNull(parsedToken.getEncryptedRoles());

        Set<String> decryptedRoles = tokenService.decryptRoles(parsedToken.getEncryptedRoles());
        assertEquals(2, decryptedRoles.size());
        assertTrue(decryptedRoles.contains("ADMIN"));
        assertTrue(decryptedRoles.contains("TEACHER"));
    }

    @Test
    void testDecryptRolesEmptyOrNull() {
        Set<String> emptyDecrypted = tokenService.decryptRoles("");
        assertTrue(emptyDecrypted.isEmpty());

        Set<String> nullDecrypted = tokenService.decryptRoles(null);
        assertTrue(nullDecrypted.isEmpty());
    }
}
