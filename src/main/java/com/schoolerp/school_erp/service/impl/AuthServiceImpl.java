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

import com.schoolerp.school_erp.security.TokenService;
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
    private TokenService tokenService;

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
        User user = userRepository.findBySchoolIdAndMobileNoAndDeletedAtIsNull(schoolId, request.getMobileNo())
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
                log.warn("User account locked due to excessive failed logins: {}", user.getMobileNo());
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
            if (Boolean.TRUE.equals(user.getOtpUsed())) {
                throw new IllegalStateException("OTP login is only allowed once and has already been used.");
            }
            String otpCode = String.valueOf((int) (Math.random() * 900000) + 100000); // 6 digit OTP
            user.setLoginOtp(otpCode);
            user.setLoginOtpExpiresAt(OffsetDateTime.now().plusMinutes(5)); // Valid for 5 minutes
            user.setOtpResendCount(0); // Initialize resend count
            userRepository.save(user);

            NotificationService whatsapp = notificationFactory.getService("whatsapp");
            whatsapp.sendOTP(user.getMobileNo(), otpCode);
            
            return LoginResponse.builder()
                    .email(user.getEmail())
                    .mobileNo(user.getMobileNo())
                    .status("MFA_REQUIRED")
                    .schoolName(user.getSchool().getName())
                    .build();
        }

        return LoginResponse.builder()
                .token(tokenService.generateToken(user.getId(), user.getRoles().stream().map(Role::getName).collect(Collectors.toSet())))
                .email(user.getEmail())
                .mobileNo(user.getMobileNo())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .schoolName(user.getSchool().getName())
                .status("SUCCESS")
                .build();
    }

    @Override
    @Transactional
    public boolean verifyOTP(String mobileNo, String code) {
        log.info("Verifying WhatsApp OTP: {} for mobileNo: {}", code, mobileNo);
        UUID schoolId = TenantContext.getCurrentTenant();
        if (schoolId == null) {
            throw new IllegalStateException("Tenant School ID must be provided in request header.");
        }

        User user = userRepository.findBySchoolIdAndMobileNoAndDeletedAtIsNull(schoolId, mobileNo)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (Boolean.TRUE.equals(user.getOtpUsed())) {
            log.warn("OTP login has already been used for user: {}", mobileNo);
            return false;
        }

        if (user.getLoginOtp() == null || user.getLoginOtpExpiresAt() == null) {
            log.warn("No active OTP request found for user: {}", mobileNo);
            return false;
        }

        if (user.getLoginOtpExpiresAt().isBefore(OffsetDateTime.now())) {
            log.warn("OTP has expired for user: {}", mobileNo);
            return false;
        }

        if (!user.getLoginOtp().equals(code)) {
            log.warn("OTP code mismatch for user: {}", mobileNo);
            return false;
        }

        // Clear OTP on successful validation and mark as used
        user.setLoginOtp(null);
        user.setLoginOtpExpiresAt(null);
        user.setOtpUsed(true);
        user.setOtpResendCount(0); // Reset count
        userRepository.save(user);
        return true;
    }

    @Override
    @Transactional
    public boolean sendOTP(String mobileNo) {
        UUID schoolId = TenantContext.getCurrentTenant();
        if (schoolId == null) {
            throw new IllegalStateException("Tenant School ID must be provided in request header.");
        }

        User user = userRepository.findBySchoolIdAndMobileNoAndOtpUsedFalseAndDeletedAtIsNull(schoolId, mobileNo)
                .orElseThrow(() -> new IllegalArgumentException("User not found with this mobile number or OTP login has already been used."));

        log.info("Generating login OTP for mobile: {} under school: {}", mobileNo, schoolId);
        String otpCode = String.valueOf((int) (Math.random() * 900000) + 100000); // 6 digit OTP
        
        user.setLoginOtp(otpCode);
        user.setLoginOtpExpiresAt(OffsetDateTime.now().plusMinutes(5));
        user.setOtpResendCount(0); // Initialize resend count
        userRepository.save(user);

        notificationFactory.getService("whatsapp").sendOTP(mobileNo, otpCode);
        return true;
    }

    @Override
    @Transactional
    public boolean resendOTP(String mobileNo) {
        UUID schoolId = TenantContext.getCurrentTenant();
        if (schoolId == null) {
            throw new IllegalStateException("Tenant School ID must be provided in request header.");
        }

        User user = userRepository.findBySchoolIdAndMobileNoAndOtpUsedFalseAndDeletedAtIsNull(schoolId, mobileNo)
                .orElseThrow(() -> new IllegalArgumentException("User not found with this mobile number or OTP login has already been used."));

        if (user.getLoginOtp() == null) {
            throw new IllegalStateException("No active OTP request found. Please request a new OTP first.");
        }

        if (user.getOtpResendCount() >= 3) {
            throw new IllegalStateException("Maximum resend attempts reached. Please request a new OTP.");
        }

        log.info("Resending login OTP for mobile: {} under school: {}, attempt {}/3", mobileNo, schoolId, user.getOtpResendCount() + 1);
        String otpCode = String.valueOf((int) (Math.random() * 900000) + 100000); // Generate new 6-digit OTP
        
        user.setLoginOtp(otpCode);
        user.setLoginOtpExpiresAt(OffsetDateTime.now().plusMinutes(5));
        user.setOtpResendCount(user.getOtpResendCount() + 1);
        userRepository.save(user);

        notificationFactory.getService("whatsapp").sendOTP(mobileNo, otpCode);
        return true;
    }

    @Override
    @Transactional
    public boolean forgotPassword(String mobileNo) {
        UUID schoolId = TenantContext.getCurrentTenant();
        if (schoolId == null) {
            throw new IllegalStateException("Tenant School ID must be provided in request header.");
        }
        User user = userRepository.findBySchoolIdAndMobileNoAndDeletedAtIsNull(schoolId, mobileNo)
                .orElseThrow(() -> new IllegalArgumentException("User not found with this mobile number."));

        if (user.getForgotPasswordCount() >= 5) {
            throw new IllegalStateException("Maximum forgot password attempts reached (5 times).");
        }

        log.info("Generating forgot password WhatsApp OTP for mobile: {}", mobileNo);
        String otpCode = String.valueOf((int) (Math.random() * 900000) + 100000); // 6 digit OTP
        
        user.setLoginOtp(otpCode);
        user.setLoginOtpExpiresAt(OffsetDateTime.now().plusMinutes(5));
        user.setForgotPasswordCount(user.getForgotPasswordCount() + 1);
        userRepository.save(user);

        notificationFactory.getService("whatsapp").sendOTP(mobileNo, otpCode);
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
