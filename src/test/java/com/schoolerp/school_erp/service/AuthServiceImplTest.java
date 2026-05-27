package com.schoolerp.school_erp.service;

import com.schoolerp.school_erp.dto.LoginRequest;
import com.schoolerp.school_erp.dto.LoginResponse;
import com.schoolerp.school_erp.entity.School;
import com.schoolerp.school_erp.entity.User;
import com.schoolerp.school_erp.filter.TenantContext;
import com.schoolerp.school_erp.repository.UserRepository;
import com.schoolerp.school_erp.security.TokenService;
import com.schoolerp.school_erp.service.impl.AuthServiceImpl;
import com.schoolerp.school_erp.strategy.NotificationFactory;
import com.schoolerp.school_erp.strategy.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private NotificationFactory notificationFactory;

    @Mock
    private NotificationService notificationService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private UUID schoolId;
    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        schoolId = UUID.randomUUID();
        TenantContext.setCurrentTenant(schoolId);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@school.com");
        user.setMobileNo("9876543210");
        user.setPasswordHash(encoder.encode("password"));
        user.setIsActive(true);
        user.setOtpUsed(false);
        user.setFailedLoginAttempts(0);
        user.setRoles(Collections.emptySet());
        user.setSchool(School.builder().name("Test School").build());

        when(notificationFactory.getService("whatsapp")).thenReturn(notificationService);
    }

    @Test
    void testLogin_Success() {
        when(userRepository.findBySchoolIdAndMobileNoAndDeletedAtIsNull(schoolId, "9876543210"))
                .thenReturn(Optional.of(user));
        when(tokenService.generateToken(any(), any())).thenReturn("mock-token");

        LoginRequest request = new LoginRequest("9876543210", "password");
        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("mock-token", response.getToken());
        assertEquals("test@school.com", response.getEmail());
        assertEquals("9876543210", response.getMobileNo());
    }

    @Test
    void testLogin_InvalidCredentials() {
        when(userRepository.findBySchoolIdAndMobileNoAndDeletedAtIsNull(schoolId, "9876543210"))
                .thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest("9876543210", "wrong-password");

        assertThrows(IllegalArgumentException.class, () -> {
            authService.login(request);
        });
    }

    @Test
    void testSendOTP_Success() {
        when(userRepository.findBySchoolIdAndMobileNoAndOtpUsedFalseAndDeletedAtIsNull(schoolId, "9876543210"))
                .thenReturn(Optional.of(user));

        boolean result = authService.sendOTP("9876543210");

        assertTrue(result);
        assertNotNull(user.getLoginOtp());
        assertNotNull(user.getLoginOtpExpiresAt());
        assertTrue(user.getLoginOtpExpiresAt().isAfter(OffsetDateTime.now()));
        verify(userRepository).save(user);
        verify(notificationService).sendOTP(eq("9876543210"), eq(user.getLoginOtp()));
    }

    @Test
    void testVerifyOTP_Success() {
        user.setLoginOtp("123456");
        user.setLoginOtpExpiresAt(OffsetDateTime.now().plusMinutes(5));

        when(userRepository.findBySchoolIdAndMobileNoAndDeletedAtIsNull(schoolId, "9876543210"))
                .thenReturn(Optional.of(user));

        boolean result = authService.verifyOTP("9876543210", "123456");

        assertTrue(result);
        assertNull(user.getLoginOtp());
        assertNull(user.getLoginOtpExpiresAt());
        assertTrue(user.getOtpUsed());
        verify(userRepository).save(user);
    }

    @Test
    void testVerifyOTP_AlreadyUsed() {
        user.setOtpUsed(true);
        user.setLoginOtp("123456");
        user.setLoginOtpExpiresAt(OffsetDateTime.now().plusMinutes(5));

        when(userRepository.findBySchoolIdAndMobileNoAndDeletedAtIsNull(schoolId, "9876543210"))
                .thenReturn(Optional.of(user));

        boolean result = authService.verifyOTP("9876543210", "123456");

        assertFalse(result);
    }

    @Test
    void testVerifyOTP_Expired() {
        user.setLoginOtp("123456");
        user.setLoginOtpExpiresAt(OffsetDateTime.now().minusMinutes(1)); // Already expired

        when(userRepository.findBySchoolIdAndMobileNoAndDeletedAtIsNull(schoolId, "9876543210"))
                .thenReturn(Optional.of(user));

        boolean result = authService.verifyOTP("9876543210", "123456");

        assertFalse(result);
        assertEquals("123456", user.getLoginOtp()); // Shouldn't clear since it failed
    }

    @Test
    void testVerifyOTP_Mismatch() {
        user.setLoginOtp("123456");
        user.setLoginOtpExpiresAt(OffsetDateTime.now().plusMinutes(5));

        when(userRepository.findBySchoolIdAndMobileNoAndDeletedAtIsNull(schoolId, "9876543210"))
                .thenReturn(Optional.of(user));

        boolean result = authService.verifyOTP("9876543210", "999999");

        assertFalse(result);
    }

    @Test
    void testResendOTP_Success() {
        user.setLoginOtp("123456");
        user.setOtpResendCount(1);
        when(userRepository.findBySchoolIdAndMobileNoAndOtpUsedFalseAndDeletedAtIsNull(schoolId, "9876543210"))
                .thenReturn(Optional.of(user));

        boolean result = authService.resendOTP("9876543210");

        assertTrue(result);
        assertEquals(2, user.getOtpResendCount());
        assertNotEquals("123456", user.getLoginOtp()); // generated new code
        verify(userRepository).save(user);
        verify(notificationService).sendOTP(eq("9876543210"), eq(user.getLoginOtp()));
    }

    @Test
    void testResendOTP_MaxAttemptsReached() {
        user.setLoginOtp("123456");
        user.setOtpResendCount(3);
        when(userRepository.findBySchoolIdAndMobileNoAndOtpUsedFalseAndDeletedAtIsNull(schoolId, "9876543210"))
                .thenReturn(Optional.of(user));

        assertThrows(IllegalStateException.class, () -> {
            authService.resendOTP("9876543210");
        });
    }

    @Test
    void testResendOTP_NoActiveSession() {
        user.setLoginOtp(null); // No active session
        when(userRepository.findBySchoolIdAndMobileNoAndOtpUsedFalseAndDeletedAtIsNull(schoolId, "9876543210"))
                .thenReturn(Optional.of(user));

        assertThrows(IllegalStateException.class, () -> {
            authService.resendOTP("9876543210");
        });
    }
}
