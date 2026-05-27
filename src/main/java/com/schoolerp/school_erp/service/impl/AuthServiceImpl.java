package com.schoolerp.school_erp.service.impl;

import com.schoolerp.school_erp.dto.LoginRequest;
import com.schoolerp.school_erp.dto.LoginResponse;
import com.schoolerp.school_erp.entity.Role;
import com.schoolerp.school_erp.entity.User;
import com.schoolerp.school_erp.filter.TenantContext;
import com.schoolerp.school_erp.repository.UserRepository;
import com.schoolerp.school_erp.service.AuthService;
import com.schoolerp.school_erp.strategy.NotificationFactory;
import com.schoolerp.school_erp.strategy.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationFactory notificationFactory;

    @Value("${school.auth.mfa.enabled:false}")
    private boolean mfaEnabled;

    @Value("${school.auth.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        UUID schoolId = TenantContext.getCurrentTenant();
        if (schoolId == null) {
            throw new IllegalStateException("Tenant School ID must be provided in request header.");
        }

        User user = userRepository.findBySchoolIdAndEmailAndDeletedAtIsNull(schoolId, request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        // Check Lockout
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(OffsetDateTime.now())) {
            throw new IllegalStateException("Account is temporarily locked. Try again later.");
        }

        // Validate Password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= maxFailedAttempts) {
                user.setLockedUntil(OffsetDateTime.now().plusMinutes(15)); // Lock for 15 mins
                log.warn("User account locked due to excessive failed logins: {}", user.getEmail());
            }
            userRepository.save(user);
            throw new IllegalArgumentException("Invalid username or password");
        }

        // Reset failed login count
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        // Check MFA
        if (mfaEnabled) {
            String otpCode = String.valueOf((int) (Math.random() * 900000) + 100000); // 6 digit OTP
            NotificationService whatsapp = notificationFactory.getService("whatsapp");
            whatsapp.sendOTP(user.getEmail(), otpCode);
            
            return LoginResponse.builder()
                    .email(user.getEmail())
                    .status("MFA_REQUIRED")
                    .schoolName(user.getSchool().getName())
                    .build();
        }

        return LoginResponse.builder()
                .token("mock-jwt-token-for-" + user.getId())
                .email(user.getEmail())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .schoolName(user.getSchool().getName())
                .status("SUCCESS")
                .build();
    }

    @Override
    public boolean verifyOTP(String email, String code) {
        log.info("Verifying WhatsApp OTP: {} for email: {}", code, email);
        // OTP validation logic mapping verified credentials
        return "123456".equals(code) || code.length() == 6; // Stub validation
    }

    @Override
    public boolean forgotPassword(String email) {
        UUID schoolId = TenantContext.getCurrentTenant();
        User user = userRepository.findBySchoolIdAndEmailAndDeletedAtIsNull(schoolId, email)
                .orElseThrow(() -> new IllegalArgumentException("Email address not found."));

        // Validate count limits (can verify reset log counts per month)
        log.info("Generating forgot password WhatsApp OTP for user: {}", user.getEmail());
        String otpCode = "555555"; // Stub reset code
        notificationFactory.getService("whatsapp").sendOTP(user.getEmail(), otpCode);
        return true;
    }

    @Override
    @Transactional
    public boolean changePassword(String email, String oldPassword, String newPassword) {
        UUID schoolId = TenantContext.getCurrentTenant();
        User user = userRepository.findBySchoolIdAndEmailAndDeletedAtIsNull(schoolId, email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid old password");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }
}
