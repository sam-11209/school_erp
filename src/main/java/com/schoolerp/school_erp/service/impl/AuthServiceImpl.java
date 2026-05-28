package com.schoolerp.school_erp.service.impl;

import com.schoolerp.school_erp.dto.*;
import com.schoolerp.school_erp.entity.Role;
import com.schoolerp.school_erp.entity.School;
import com.schoolerp.school_erp.entity.User;
import com.schoolerp.school_erp.filter.TenantContext;
import com.schoolerp.school_erp.repository.UserRepository;
import com.schoolerp.school_erp.repository.SchoolRepository;
import com.schoolerp.school_erp.repository.RoleRepository;
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
    private SchoolRepository schoolRepository;

    @Autowired
    private RoleRepository roleRepository;

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

        User user;
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            user = userRepository.findBySchoolIdAndEmailAndDeletedAtIsNull(schoolId, request.getEmail().trim())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));
        } else if (request.getMobileNo() != null && !request.getMobileNo().trim().isEmpty()) {
            user = userRepository.findBySchoolIdAndMobileNoAndDeletedAtIsNull(schoolId, request.getMobileNo().trim())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));
        } else {
            throw new IllegalArgumentException("Either email or mobile number must be provided");
        }

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
                log.warn("User account locked due to excessive failed logins: {}", user.getMobileNo() != null ? user.getMobileNo() : user.getEmail());
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
    public boolean register(RegisterRequest request) {
        log.info("Registering user/school: fullName={}, mobileNo={}, schoolName={}, email={}",
                request.getFullName(), request.getMobileNo(), request.getSchoolName(), request.getEmailAddress());

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Generate unique subdomain from School Name
        String baseSubdomain = request.getSchoolName().toLowerCase()
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        
        if (baseSubdomain.isEmpty()) {
            baseSubdomain = "school-" + UUID.randomUUID().toString().substring(0, 8);
        }

        String subdomain = baseSubdomain;
        int suffix = 1;
        while (schoolRepository.findBySubdomainAndDeletedAtIsNull(subdomain).isPresent()) {
            subdomain = baseSubdomain + "-" + suffix;
            suffix++;
        }

        // Create and save School
        School school = School.builder()
                .name(request.getSchoolName().trim())
                .subdomain(subdomain)
                .isActive(true)
                .build();
        school = schoolRepository.save(school);

        // Create and save default Admin Role for this School
        Role adminRole = Role.builder()
                .school(school)
                .name("ADMIN")
                .description("School Administrator")
                .isSystemRole(true)
                .build();
        adminRole = roleRepository.save(adminRole);

        // Verify that user does not already exist for this school (in case they have same email/mobile)
        if (userRepository.findBySchoolIdAndEmailAndDeletedAtIsNull(school.getId(), request.getEmailAddress().trim()).isPresent()) {
            throw new IllegalArgumentException("User with this email already exists for this school.");
        }
        if (userRepository.findBySchoolIdAndMobileNoAndDeletedAtIsNull(school.getId(), request.getMobileNo().trim()).isPresent()) {
            throw new IllegalArgumentException("User with this mobile number already exists for this school.");
        }

        // Create and save User
        User user = User.builder()
                .school(school)
                .fullName(request.getFullName().trim())
                .email(request.getEmailAddress().trim())
                .mobileNo(request.getMobileNo().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .isActive(true)
                .roles(java.util.Set.of(adminRole))
                .build();
        userRepository.save(user);

        log.info("Successfully registered school {} (subdomain: {}) and admin user {}", school.getName(), subdomain, user.getEmail());
        return true;
    }

    @Override
    @Transactional
    public boolean verifyOTP(VerifyOtpRequest request) {
        log.info("Verifying OTP for request: {}", request);
        UUID schoolId = TenantContext.getCurrentTenant();
        if (schoolId == null) {
            throw new IllegalStateException("Tenant School ID must be provided in request header.");
        }

        String otpIdStr = request.getOtpId();
        if (otpIdStr == null || otpIdStr.trim().isEmpty()) {
            throw new IllegalArgumentException("otpId is required for verification");
        }
        UUID otpId;
        try {
            otpId = UUID.fromString(otpIdStr);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid otpId UUID format: {}", otpIdStr);
            return false;
        }

        String emailId = request.getEmailId();
        String mobileNo = request.getMobileNo();

        User user = null;
        if (emailId != null && !emailId.trim().isEmpty()) {
            user = userRepository.findBySchoolIdAndEmailAndDeletedAtIsNull(schoolId, emailId.trim())
                    .orElse(null);
        }

        if (user == null && mobileNo != null && !mobileNo.trim().isEmpty()) {
            user = userRepository.findBySchoolIdAndMobileNoAndDeletedAtIsNull(schoolId, mobileNo.trim())
                    .orElse(null);
        }

        if (user == null) {
            log.warn("User not found for verify-otp request: emailId={}, mobileNo={}", emailId, mobileNo);
            return false;
        }

        if (!otpId.equals(user.getLoginOtpId())) {
            log.warn("otpId mismatch: expected {}, got {}", user.getLoginOtpId(), otpId);
            return false;
        }

        if (Boolean.TRUE.equals(user.getOtpUsed())) {
            log.warn("OTP login has already been used for user: {}", user.getId());
            return false;
        }

        if (user.getLoginOtp() == null || user.getLoginOtpExpiresAt() == null) {
            log.warn("No active OTP request found for user: {}", user.getId());
            return false;
        }

        if (user.getLoginOtpExpiresAt().isBefore(OffsetDateTime.now())) {
            log.warn("OTP has expired for user: {}", user.getId());
            return false;
        }

        if (!user.getLoginOtp().equals(request.getCode())) {
            log.warn("OTP code mismatch");
            return false;
        }

        // Clear OTP on successful validation and mark as used
        user.setLoginOtp(null);
        user.setLoginOtpId(null);
        user.setLoginOtpExpiresAt(null);
        user.setOtpUsed(true);
        user.setOtpResendCount(0); // Reset count
        userRepository.save(user);
        return true;
    }

    @Override
    @Transactional
    public SendOtpResponse sendOTP(SendOtpRequest request) {
        UUID schoolId = TenantContext.getCurrentTenant();
        if (schoolId == null) {
            throw new IllegalStateException("Tenant School ID must be provided in request header.");
        }

        String emailId = request.getEmailId();
        String mobileNo = request.getMobileNo();

        if ((emailId == null || emailId.trim().isEmpty()) && (mobileNo == null || mobileNo.trim().isEmpty())) {
            throw new IllegalArgumentException("Either email or mobile number must be provided");
        }

        User user;
        if (emailId != null && !emailId.trim().isEmpty()) {
            user = userRepository.findBySchoolIdAndEmailAndDeletedAtIsNull(schoolId, emailId.trim())
                    .orElseThrow(() -> new IllegalArgumentException("User not found with this email."));
        } else {
            user = userRepository.findBySchoolIdAndMobileNoAndDeletedAtIsNull(schoolId, mobileNo.trim())
                    .orElseThrow(() -> new IllegalArgumentException("User not found with this mobile number."));
        }

        if (Boolean.TRUE.equals(user.getOtpUsed())) {
            throw new IllegalStateException("OTP login is only allowed once and has already been used.");
        }

        UUID otpId = UUID.randomUUID();
        String otpCode = String.valueOf((int) (Math.random() * 900000) + 100000); // 6 digit OTP

        user.setLoginOtp(otpCode);
        user.setLoginOtpId(otpId);
        user.setLoginOtpExpiresAt(OffsetDateTime.now().plusMinutes(5));
        user.setOtpResendCount(0); // Initialize resend count
        userRepository.save(user);

        SendOtpResponse.SendOtpResponseBuilder responseBuilder = SendOtpResponse.builder().otpId(otpId.toString());

        if (request.isSendEmail() && emailId != null && !emailId.trim().isEmpty()) {
            notificationFactory.getService("email").sendOTP(emailId.trim(), otpCode);
            responseBuilder.emailId(emailId.trim());
        }

        if (request.isSendSms() && mobileNo != null && !mobileNo.trim().isEmpty()) {
            notificationFactory.getService("whatsapp").sendOTP(mobileNo.trim(), otpCode);
            responseBuilder.mobileNo(mobileNo.trim());
        }

        return responseBuilder.build();
    }

    @Override
    @Transactional
    public SendOtpResponse resendOTP(ResendOtpRequest request) {
        log.info("Resending OTP for request: {}", request);
        UUID schoolId = TenantContext.getCurrentTenant();
        if (schoolId == null) {
            throw new IllegalStateException("Tenant School ID must be provided in request header.");
        }

        String otpIdStr = request.getOtpId();
        if (otpIdStr == null || otpIdStr.trim().isEmpty()) {
            throw new IllegalArgumentException("otpId is required for resending OTP");
        }
        UUID otpId;
        try {
            otpId = UUID.fromString(otpIdStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid otpId UUID format: " + otpIdStr);
        }

        String emailId = request.getEmailId();
        String mobileNo = request.getMobileNo();

        User user = null;
        if (emailId != null && !emailId.trim().isEmpty()) {
            user = userRepository.findBySchoolIdAndEmailAndDeletedAtIsNull(schoolId, emailId.trim())
                    .orElse(null);
        }

        if (user == null && mobileNo != null && !mobileNo.trim().isEmpty()) {
            user = userRepository.findBySchoolIdAndMobileNoAndDeletedAtIsNull(schoolId, mobileNo.trim())
                    .orElse(null);
        }

        if (user == null) {
            throw new IllegalArgumentException("User not found with matching email or mobile number.");
        }

        if (!otpId.equals(user.getLoginOtpId())) {
            throw new IllegalArgumentException("Invalid otpId session.");
        }

        if (user.getLoginOtp() == null) {
            throw new IllegalStateException("No active OTP request found. Please request a new OTP first.");
        }

        if (user.getOtpResendCount() >= 3) {
            throw new IllegalStateException("Maximum resend attempts reached. Please request a new OTP.");
        }

        log.info("Resending OTP for user: {}, attempt {}/3", user.getId(), user.getOtpResendCount() + 1);
        String otpCode = String.valueOf((int) (Math.random() * 900000) + 100000); // Generate new 6-digit OTP
        
        user.setLoginOtp(otpCode);
        user.setLoginOtpExpiresAt(OffsetDateTime.now().plusMinutes(5));
        user.setOtpResendCount(user.getOtpResendCount() + 1);
        userRepository.save(user);

        SendOtpResponse.SendOtpResponseBuilder responseBuilder = SendOtpResponse.builder().otpId(otpId.toString());

        boolean resendToEmail = request.isSendEmail();
        boolean resendToSms = request.isSendSms();

        // Fallback if neither flag is set to true
        if (!resendToEmail && !resendToSms) {
            resendToEmail = (emailId != null && !emailId.trim().isEmpty());
            resendToSms = (mobileNo != null && !mobileNo.trim().isEmpty());
        }

        if (resendToEmail && emailId != null && !emailId.trim().isEmpty()) {
            notificationFactory.getService("email").sendOTP(emailId.trim(), otpCode);
            responseBuilder.emailId(emailId.trim());
        }

        if (resendToSms && mobileNo != null && !mobileNo.trim().isEmpty()) {
            notificationFactory.getService("whatsapp").sendOTP(mobileNo.trim(), otpCode);
            responseBuilder.mobileNo(mobileNo.trim());
        }

        return responseBuilder.build();
    }

    @Override
    @Transactional
    public boolean forgotPassword(String email, String mobileNo) {
        UUID schoolId = TenantContext.getCurrentTenant();
        if (schoolId == null) {
            throw new IllegalStateException("Tenant School ID must be provided in request header.");
        }

        if ((email == null || email.trim().isEmpty()) && (mobileNo == null || mobileNo.trim().isEmpty())) {
            throw new IllegalArgumentException("Either email or mobile number must be provided");
        }

        User user;
        if (email != null && !email.trim().isEmpty()) {
            user = userRepository.findBySchoolIdAndEmailAndDeletedAtIsNull(schoolId, email.trim())
                    .orElseThrow(() -> new IllegalArgumentException("User not found with this email."));
        } else {
            user = userRepository.findBySchoolIdAndMobileNoAndDeletedAtIsNull(schoolId, mobileNo.trim())
                    .orElseThrow(() -> new IllegalArgumentException("User not found with this mobile number."));
        }

        if (user.getForgotPasswordCount() >= 5) {
            throw new IllegalStateException("Maximum forgot password attempts reached (5 times).");
        }

        log.info("Generating forgot password OTP for user: {}", (email != null && !email.trim().isEmpty()) ? email : mobileNo);
        String otpCode = String.valueOf((int) (Math.random() * 900000) + 100000); // 6 digit OTP
        
        user.setLoginOtp(otpCode);
        user.setLoginOtpExpiresAt(OffsetDateTime.now().plusMinutes(5));
        user.setForgotPasswordCount(user.getForgotPasswordCount() + 1);
        userRepository.save(user);

        if (email != null && !email.trim().isEmpty()) {
            notificationFactory.getService("email").sendOTP(email.trim(), otpCode);
        } else {
            notificationFactory.getService("whatsapp").sendOTP(mobileNo.trim(), otpCode);
        }
        return true;
    }

    @Override
    @Transactional
    public boolean resetPassword(ResetPasswordRequest request) {
        log.info("Resetting password for request: {}", request);
        UUID schoolId = TenantContext.getCurrentTenant();
        if (schoolId == null) {
            throw new IllegalStateException("Tenant School ID must be provided in request header.");
        }

        String emailId = request.getEmailId();
        String mobileNo = request.getMobileNo();

        if ((emailId == null || emailId.trim().isEmpty()) && (mobileNo == null || mobileNo.trim().isEmpty())) {
            throw new IllegalArgumentException("Either email or mobile number must be provided");
        }

        User user = null;
        if (emailId != null && !emailId.trim().isEmpty()) {
            user = userRepository.findBySchoolIdAndEmailAndDeletedAtIsNull(schoolId, emailId.trim())
                    .orElse(null);
        }

        if (user == null && mobileNo != null && !mobileNo.trim().isEmpty()) {
            user = userRepository.findBySchoolIdAndMobileNoAndDeletedAtIsNull(schoolId, mobileNo.trim())
                    .orElse(null);
        }

        if (user == null) {
            throw new IllegalArgumentException("User not found with matching email or mobile number.");
        }

        if (user.getLoginOtp() == null || user.getLoginOtpExpiresAt() == null) {
            throw new IllegalStateException("No active OTP request found. Please request a new OTP first.");
        }

        if (user.getLoginOtpExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalStateException("OTP has expired. Please request a new OTP.");
        }

        if (!user.getLoginOtp().equals(request.getCode())) {
            throw new IllegalArgumentException("Invalid OTP code.");
        }

        // OTP is valid! Let's update password and reset lockout & failed attempts
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setLoginOtp(null);
        user.setLoginOtpId(null);
        user.setLoginOtpExpiresAt(null);
        user.setForgotPasswordCount(0); // Reset count on successful password reset
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        log.info("Password reset successfully for user: {}", user.getId());
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
