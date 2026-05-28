package com.schoolerp.school_erp.service;

import com.schoolerp.school_erp.dto.*;
import com.schoolerp.school_erp.entity.School;
import com.schoolerp.school_erp.entity.User;
import com.schoolerp.school_erp.entity.Role;
import com.schoolerp.school_erp.filter.TenantContext;
import com.schoolerp.school_erp.repository.UserRepository;
import com.schoolerp.school_erp.repository.SchoolRepository;
import com.schoolerp.school_erp.repository.RoleRepository;
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
    private SchoolRepository schoolRepository;

    @Mock
    private RoleRepository roleRepository;

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

        LoginRequest request = new LoginRequest(null, "9876543210", "password");
        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("mock-token", response.getToken());
        assertEquals("test@school.com", response.getEmail());
        assertEquals("9876543210", response.getMobileNo());
    }

    @Test
    void testLogin_Success_WithEmail() {
        when(userRepository.findBySchoolIdAndEmailAndDeletedAtIsNull(schoolId, "test@school.com"))
                .thenReturn(Optional.of(user));
        when(tokenService.generateToken(any(), any())).thenReturn("mock-token");

        LoginRequest request = new LoginRequest("test@school.com", null, "password");
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

        LoginRequest request = new LoginRequest(null, "9876543210", "wrong-password");

        assertThrows(IllegalArgumentException.class, () -> {
            authService.login(request);
        });
    }

    @Test
    void testSendOTP_Success() {
        when(userRepository.findBySchoolIdAndMobileNoAndDeletedAtIsNull(schoolId, "9876543210"))
                .thenReturn(Optional.of(user));

        SendOtpRequest request = new SendOtpRequest("9876543210", true, null, false);
        SendOtpResponse response = authService.sendOTP(request);

        assertNotNull(response);
        assertNotNull(response.getOtpId());
        assertEquals("9876543210", response.getMobileNo());
        assertNull(response.getEmailId());
        assertNotNull(user.getLoginOtp());
        assertNotNull(user.getLoginOtpExpiresAt());
        assertTrue(user.getLoginOtpExpiresAt().isAfter(OffsetDateTime.now()));
        verify(userRepository).save(user);
        verify(notificationService).sendOTP(eq("9876543210"), eq(user.getLoginOtp()));
    }

    @Test
    void testVerifyOTP_Success() {
        UUID otpId = UUID.randomUUID();
        user.setLoginOtp("123456");
        user.setLoginOtpId(otpId);
        user.setLoginOtpExpiresAt(OffsetDateTime.now().plusMinutes(5));

        when(userRepository.findBySchoolIdAndMobileNoAndDeletedAtIsNull(schoolId, "9876543210"))
                .thenReturn(Optional.of(user));

        VerifyOtpRequest request = new VerifyOtpRequest(otpId.toString(), "9876543210", null, "123456");
        boolean result = authService.verifyOTP(request);

        assertTrue(result);
        assertNull(user.getLoginOtp());
        assertNull(user.getLoginOtpId());
        assertNull(user.getLoginOtpExpiresAt());
        assertTrue(user.getOtpUsed());
        verify(userRepository).save(user);
    }

    @Test
    void testVerifyOTP_AlreadyUsed() {
        UUID otpId = UUID.randomUUID();
        user.setOtpUsed(true);
        user.setLoginOtp("123456");
        user.setLoginOtpId(otpId);
        user.setLoginOtpExpiresAt(OffsetDateTime.now().plusMinutes(5));

        when(userRepository.findBySchoolIdAndMobileNoAndDeletedAtIsNull(schoolId, "9876543210"))
                .thenReturn(Optional.of(user));

        VerifyOtpRequest request = new VerifyOtpRequest(otpId.toString(), "9876543210", null, "123456");
        boolean result = authService.verifyOTP(request);

        assertFalse(result);
    }

    @Test
    void testVerifyOTP_Expired() {
        UUID otpId = UUID.randomUUID();
        user.setLoginOtp("123456");
        user.setLoginOtpId(otpId);
        user.setLoginOtpExpiresAt(OffsetDateTime.now().minusMinutes(1)); // Already expired

        when(userRepository.findBySchoolIdAndMobileNoAndDeletedAtIsNull(schoolId, "9876543210"))
                .thenReturn(Optional.of(user));

        VerifyOtpRequest request = new VerifyOtpRequest(otpId.toString(), "9876543210", null, "123456");
        boolean result = authService.verifyOTP(request);

        assertFalse(result);
        assertEquals("123456", user.getLoginOtp()); // Shouldn't clear since it failed
    }

    @Test
    void testVerifyOTP_Mismatch() {
        UUID otpId = UUID.randomUUID();
        user.setLoginOtp("123456");
        user.setLoginOtpId(otpId);
        user.setLoginOtpExpiresAt(OffsetDateTime.now().plusMinutes(5));

        when(userRepository.findBySchoolIdAndMobileNoAndDeletedAtIsNull(schoolId, "9876543210"))
                .thenReturn(Optional.of(user));

        VerifyOtpRequest request = new VerifyOtpRequest(otpId.toString(), "9876543210", null, "999999");
        boolean result = authService.verifyOTP(request);

        assertFalse(result);
    }

    @Test
    void testResendOTP_Success() {
        UUID otpId = UUID.randomUUID();
        user.setLoginOtp("123456");
        user.setLoginOtpId(otpId);
        user.setOtpResendCount(1);
        when(userRepository.findBySchoolIdAndMobileNoAndDeletedAtIsNull(schoolId, "9876543210"))
                .thenReturn(Optional.of(user));

        ResendOtpRequest request = new ResendOtpRequest(otpId.toString(), "9876543210", null);
        SendOtpResponse response = authService.resendOTP(request);

        assertNotNull(response);
        assertEquals(otpId.toString(), response.getOtpId());
        assertEquals("9876543210", response.getMobileNo());
        assertEquals(2, user.getOtpResendCount());
        assertNotEquals("123456", user.getLoginOtp()); // generated new code
        verify(userRepository).save(user);
        verify(notificationService).sendOTP(eq("9876543210"), eq(user.getLoginOtp()));
    }

    @Test
    void testResendOTP_EmailOnly() {
        UUID otpId = UUID.randomUUID();
        user.setLoginOtp("123456");
        user.setLoginOtpId(otpId);
        user.setOtpResendCount(1);
        when(userRepository.findBySchoolIdAndEmailAndDeletedAtIsNull(schoolId, "test@school.com"))
                .thenReturn(Optional.of(user));
        when(notificationFactory.getService("email")).thenReturn(notificationService);

        ResendOtpRequest request = ResendOtpRequest.builder()
                .otpId(otpId.toString())
                .emailId("test@school.com")
                .sendEmail(true)
                .sendSms(false)
                .build();
        SendOtpResponse response = authService.resendOTP(request);

        assertNotNull(response);
        assertEquals(otpId.toString(), response.getOtpId());
        assertEquals("test@school.com", response.getEmailId());
        assertNull(response.getMobileNo());
        assertEquals(2, user.getOtpResendCount());
        verify(notificationService).sendOTP(eq("test@school.com"), eq(user.getLoginOtp()));
    }

    @Test
    void testResendOTP_MobileOnly() {
        UUID otpId = UUID.randomUUID();
        user.setLoginOtp("123456");
        user.setLoginOtpId(otpId);
        user.setOtpResendCount(1);
        when(userRepository.findBySchoolIdAndMobileNoAndDeletedAtIsNull(schoolId, "9876543210"))
                .thenReturn(Optional.of(user));

        ResendOtpRequest request = ResendOtpRequest.builder()
                .otpId(otpId.toString())
                .mobileNo("9876543210")
                .sendSms(true)
                .sendEmail(false)
                .build();
        SendOtpResponse response = authService.resendOTP(request);

        assertNotNull(response);
        assertEquals(otpId.toString(), response.getOtpId());
        assertEquals("9876543210", response.getMobileNo());
        assertNull(response.getEmailId());
        assertEquals(2, user.getOtpResendCount());
        verify(notificationService).sendOTP(eq("9876543210"), eq(user.getLoginOtp()));
    }

    @Test
    void testResendOTP_Both() {
        UUID otpId = UUID.randomUUID();
        user.setLoginOtp("123456");
        user.setLoginOtpId(otpId);
        user.setOtpResendCount(1);
        when(userRepository.findBySchoolIdAndEmailAndDeletedAtIsNull(schoolId, "test@school.com"))
                .thenReturn(Optional.of(user));
        when(notificationFactory.getService("email")).thenReturn(notificationService);

        ResendOtpRequest request = ResendOtpRequest.builder()
                .otpId(otpId.toString())
                .emailId("test@school.com")
                .mobileNo("9876543210")
                .sendEmail(true)
                .sendSms(true)
                .build();
        SendOtpResponse response = authService.resendOTP(request);

        assertNotNull(response);
        assertEquals(otpId.toString(), response.getOtpId());
        assertEquals("test@school.com", response.getEmailId());
        assertEquals("9876543210", response.getMobileNo());
        assertEquals(2, user.getOtpResendCount());
        verify(notificationService).sendOTP(eq("test@school.com"), eq(user.getLoginOtp()));
        verify(notificationService).sendOTP(eq("9876543210"), eq(user.getLoginOtp()));
    }

    @Test
    void testResendOTP_MaxAttemptsReached() {
        UUID otpId = UUID.randomUUID();
        user.setLoginOtp("123456");
        user.setLoginOtpId(otpId);
        user.setOtpResendCount(3);
        when(userRepository.findBySchoolIdAndMobileNoAndDeletedAtIsNull(schoolId, "9876543210"))
                .thenReturn(Optional.of(user));

        ResendOtpRequest request = new ResendOtpRequest(otpId.toString(), "9876543210", null);
        assertThrows(IllegalStateException.class, () -> {
            authService.resendOTP(request);
        });
    }

    @Test
    void testResendOTP_NoActiveSession() {
        UUID otpId = UUID.randomUUID();
        user.setLoginOtp(null); // No active session
        user.setLoginOtpId(otpId);
        when(userRepository.findBySchoolIdAndMobileNoAndDeletedAtIsNull(schoolId, "9876543210"))
                .thenReturn(Optional.of(user));

        ResendOtpRequest request = new ResendOtpRequest(otpId.toString(), "9876543210", null);
        assertThrows(IllegalStateException.class, () -> {
            authService.resendOTP(request);
        });
    }

    @Test
    void testForgotPassword_Success_Mobile() {
        user.setForgotPasswordCount(2);
        when(userRepository.findBySchoolIdAndMobileNoAndDeletedAtIsNull(schoolId, "9876543210"))
                .thenReturn(Optional.of(user));

        boolean result = authService.forgotPassword(null, "9876543210");

        assertTrue(result);
        assertEquals(3, user.getForgotPasswordCount());
        assertNotNull(user.getLoginOtp());
        assertNotNull(user.getLoginOtpExpiresAt());
        verify(userRepository).save(user);
        verify(notificationService).sendOTP(eq("9876543210"), eq(user.getLoginOtp()));
    }

    @Test
    void testForgotPassword_Success_Email() {
        user.setForgotPasswordCount(2);
        when(userRepository.findBySchoolIdAndEmailAndDeletedAtIsNull(schoolId, "test@school.com"))
                .thenReturn(Optional.of(user));
        when(notificationFactory.getService("email")).thenReturn(notificationService);

        boolean result = authService.forgotPassword("test@school.com", null);

        assertTrue(result);
        assertEquals(3, user.getForgotPasswordCount());
        assertNotNull(user.getLoginOtp());
        assertNotNull(user.getLoginOtpExpiresAt());
        verify(userRepository).save(user);
        verify(notificationService).sendOTP(eq("test@school.com"), eq(user.getLoginOtp()));
    }

    @Test
    void testForgotPassword_MaxAttemptsReached() {
        user.setForgotPasswordCount(5);
        when(userRepository.findBySchoolIdAndMobileNoAndDeletedAtIsNull(schoolId, "9876543210"))
                .thenReturn(Optional.of(user));

        assertThrows(IllegalStateException.class, () -> {
            authService.forgotPassword(null, "9876543210");
        });
    }

    @Test
    void testForgotPassword_BothNullOrEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            authService.forgotPassword(null, null);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            authService.forgotPassword("   ", "");
        });
    }

    @Test
    void testResetPassword_Success() {
        user.setLoginOtp("123456");
        user.setLoginOtpExpiresAt(OffsetDateTime.now().plusMinutes(5));
        user.setForgotPasswordCount(3);
        user.setFailedLoginAttempts(2);
        user.setLockedUntil(OffsetDateTime.now().plusMinutes(10));

        when(userRepository.findBySchoolIdAndEmailAndDeletedAtIsNull(schoolId, "test@school.com"))
                .thenReturn(Optional.of(user));

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .emailId("test@school.com")
                .code("123456")
                .newPassword("new-secure-password")
                .build();

        boolean result = authService.resetPassword(request);

        assertTrue(result);
        assertNull(user.getLoginOtp());
        assertNull(user.getLoginOtpExpiresAt());
        assertEquals(0, user.getForgotPasswordCount());
        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getLockedUntil());
        verify(userRepository).save(user);
    }

    @Test
    void testResetPassword_InvalidCode() {
        user.setLoginOtp("123456");
        user.setLoginOtpExpiresAt(OffsetDateTime.now().plusMinutes(5));

        when(userRepository.findBySchoolIdAndEmailAndDeletedAtIsNull(schoolId, "test@school.com"))
                .thenReturn(Optional.of(user));

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .emailId("test@school.com")
                .code("wrongcode")
                .newPassword("new-secure-password")
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            authService.resetPassword(request);
        });
    }

    @Test
    void testResetPassword_ExpiredCode() {
        user.setLoginOtp("123456");
        user.setLoginOtpExpiresAt(OffsetDateTime.now().minusMinutes(1)); // Expired

        when(userRepository.findBySchoolIdAndEmailAndDeletedAtIsNull(schoolId, "test@school.com"))
                .thenReturn(Optional.of(user));

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .emailId("test@school.com")
                .code("123456")
                .newPassword("new-secure-password")
                .build();

        assertThrows(IllegalStateException.class, () -> {
            authService.resetPassword(request);
        });
    }

    @Test
    void testResetPassword_NoActiveOtp() {
        user.setLoginOtp(null);
        user.setLoginOtpExpiresAt(null);

        when(userRepository.findBySchoolIdAndEmailAndDeletedAtIsNull(schoolId, "test@school.com"))
                .thenReturn(Optional.of(user));

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .emailId("test@school.com")
                .code("123456")
                .newPassword("new-secure-password")
                .build();

        assertThrows(IllegalStateException.class, () -> {
            authService.resetPassword(request);
        });
    }

    @Test
    void testRegister_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("John Doe")
                .mobileNo("9876543210")
                .schoolName("Greenwood High")
                .emailAddress("john@greenwood.com")
                .password("password123")
                .confirmPassword("password123")
                .build();

        when(schoolRepository.findBySubdomainAndDeletedAtIsNull(anyString()))
                .thenReturn(Optional.empty());

        UUID generatedSchoolId = UUID.randomUUID();
        School savedSchool = School.builder()
                .id(generatedSchoolId)
                .name("Greenwood High")
                .subdomain("greenwood-high")
                .build();
        when(schoolRepository.save(any(School.class))).thenReturn(savedSchool);

        Role savedRole = Role.builder()
                .id(UUID.randomUUID())
                .name("ADMIN")
                .school(savedSchool)
                .build();
        when(roleRepository.save(any(Role.class))).thenReturn(savedRole);

        when(userRepository.findBySchoolIdAndEmailAndDeletedAtIsNull(eq(generatedSchoolId), anyString()))
                .thenReturn(Optional.empty());
        when(userRepository.findBySchoolIdAndMobileNoAndDeletedAtIsNull(eq(generatedSchoolId), anyString()))
                .thenReturn(Optional.empty());

        boolean result = authService.register(request);

        assertTrue(result);
        verify(schoolRepository).save(any(School.class));
        verify(roleRepository).save(any(Role.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRegister_PasswordsMismatch() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("John Doe")
                .mobileNo("9876543210")
                .schoolName("Greenwood High")
                .emailAddress("john@greenwood.com")
                .password("password123")
                .confirmPassword("differentPassword")
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            authService.register(request);
        });
    }

    @Test
    void testRegister_UserAlreadyExists_Email() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("John Doe")
                .mobileNo("9876543210")
                .schoolName("Greenwood High")
                .emailAddress("john@greenwood.com")
                .password("password123")
                .confirmPassword("password123")
                .build();

        when(schoolRepository.findBySubdomainAndDeletedAtIsNull(anyString()))
                .thenReturn(Optional.empty());

        UUID generatedSchoolId = UUID.randomUUID();
        School savedSchool = School.builder()
                .id(generatedSchoolId)
                .name("Greenwood High")
                .subdomain("greenwood-high")
                .build();
        when(schoolRepository.save(any(School.class))).thenReturn(savedSchool);

        Role savedRole = Role.builder()
                .id(UUID.randomUUID())
                .name("ADMIN")
                .school(savedSchool)
                .build();
        when(roleRepository.save(any(Role.class))).thenReturn(savedRole);

        when(userRepository.findBySchoolIdAndEmailAndDeletedAtIsNull(eq(generatedSchoolId), eq("john@greenwood.com")))
                .thenReturn(Optional.of(new User()));

        assertThrows(IllegalArgumentException.class, () -> {
            authService.register(request);
        });
    }

    @Test
    void testRegister_UserAlreadyExists_Mobile() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("John Doe")
                .mobileNo("9876543210")
                .schoolName("Greenwood High")
                .emailAddress("john@greenwood.com")
                .password("password123")
                .confirmPassword("password123")
                .build();

        when(schoolRepository.findBySubdomainAndDeletedAtIsNull(anyString()))
                .thenReturn(Optional.empty());

        UUID generatedSchoolId = UUID.randomUUID();
        School savedSchool = School.builder()
                .id(generatedSchoolId)
                .name("Greenwood High")
                .subdomain("greenwood-high")
                .build();
        when(schoolRepository.save(any(School.class))).thenReturn(savedSchool);

        Role savedRole = Role.builder()
                .id(UUID.randomUUID())
                .name("ADMIN")
                .school(savedSchool)
                .build();
        when(roleRepository.save(any(Role.class))).thenReturn(savedRole);

        when(userRepository.findBySchoolIdAndEmailAndDeletedAtIsNull(eq(generatedSchoolId), eq("john@greenwood.com")))
                .thenReturn(Optional.empty());
        when(userRepository.findBySchoolIdAndMobileNoAndDeletedAtIsNull(eq(generatedSchoolId), eq("9876543210")))
                .thenReturn(Optional.of(new User()));

        assertThrows(IllegalArgumentException.class, () -> {
            authService.register(request);
        });
    }
}
