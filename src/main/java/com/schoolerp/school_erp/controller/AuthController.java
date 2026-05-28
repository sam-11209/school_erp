package com.schoolerp.school_erp.controller;

import com.schoolerp.school_erp.dto.*;
import com.schoolerp.school_erp.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * Registers a new school and its corresponding administrator user.
     * 
     * Constraints:
     * - No X-Tenant-ID header is required for this call.
     * - The request body must contain all registration details: fullName, mobileNo, schoolName, emailAddress, password, and confirmPassword.
     * - The password and confirmPassword fields must match exactly.
     * - Generates a unique school subdomain automatically from the provided schoolName.
     * - Automatically registers the newly created user under the ADMIN role for the new school.
     * 
     * @param request payload containing user and school registration details
     * @return registration success message
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        boolean registered = authService.register(request);
        if (registered) {
            return ResponseEntity.ok("Registration successful");
        }
        return ResponseEntity.badRequest().body("Registration failed");
    }

    /**
     * Authenticates a user by mobile number or email and password under a specific tenant.
     * If MFA is enabled, triggers an OTP send to the user's mobile number and returns
     * an "MFA_REQUIRED" status without generating a session token.
     * 
     * Constraints:
     * - Requires the X-Tenant-ID header to resolve the school context.
     * - Blocks MFA login if the user has already logged in with an OTP (otp_used is true).
     * - The request body must contain either email or mobileNo along with password.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Generates and sends a new 6-digit OTP code to the provided email and/or mobile number.
     * 
     * Constraints:
     * - Requires the X-Tenant-ID header.
     * - The request body must contain either emailId or mobileNo.
     * - Checks if the user belongs to the tenant school.
     * - Blocks requests if the user has already logged in with an OTP (otp_used is true).
     * - Resets the OTP resend count to 0.
     * 
     * @param request payload containing emailId, mobileNo, sendEmail, and sendSms flags
     * @return SendOtpResponse containing generated otpId and sent coordinates
     */
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOTP(@Valid @RequestBody SendOtpRequest request) {
        SendOtpResponse response = authService.sendOTP(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Resends a new 6-digit OTP code for the active OTP session.
     * 
     * Constraints:
     * - Requires the X-Tenant-ID header.
     * - Must have an active, initialized OTP session matching the provided otpId.
     * - Maximum 3 resend attempts allowed per session (resend count is tracked).
     * - Blocks requests if the user has already logged in with an OTP (otp_used is true).
     * 
     * @param request payload specifying otpId, emailId, and mobileNo coordinates
     * @return SendOtpResponse containing otpId and sent coordinates
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOTP(@Valid @RequestBody ResendOtpRequest request) {
        SendOtpResponse response = authService.resendOTP(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Verifies the provided OTP code against the database record.
     * 
     * Constraints:
     * - Requires the X-Tenant-ID header.
     * - Requires verifyOtpRequest containing otpId, code, and optional emailId/mobileNo.
     * - Checks if the stored OTP exists, matches the input code, and is not expired (expires in 5 minutes).
     * - Verifies that the user matches the provided identifier (email takes priority over mobile number).
     * - Blocks requests if the user has already logged in with an OTP (otp_used is true).
     * - On successful verification, clears the OTP fields and marks otp_used as true (preventing future OTP logins).
     * 
     * @param request payload containing verification details
     * @return verification status response
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOTP(@Valid @RequestBody VerifyOtpRequest request) {
        boolean isValid = authService.verifyOTP(request);
        if (isValid) {
            return ResponseEntity.ok("OTP verified successfully");
        }
        return ResponseEntity.badRequest().body("Invalid or expired OTP");
    }

    /**
     * Generates and sends a password reset OTP to the user's email or WhatsApp.
     * 
     * Constraints:
     * - Requires the X-Tenant-ID header.
     * - Requires either email or mobile number to identify the registered user.
     * - If email is provided, OTP is sent via email; if mobileNo is provided (and email is not), OTP is sent via WhatsApp.
     * - Allowed maximum of 5 times per user.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String mobileNo) {
        boolean sent = authService.forgotPassword(email, mobileNo);
        if (sent) {
            return ResponseEntity.ok("Password reset OTP sent successfully");
        }
        return ResponseEntity.badRequest().body("Failed to send reset link");
    }

    /**
     * Resets a user's password using the OTP code received during the forgot-password flow.
     * 
     * Constraints:
     * - Requires the X-Tenant-ID header.
     * - Requires a request body containing the OTP code and new password.
     * - Requires either emailId or mobileNo to identify the user.
     * - The OTP code must be valid, correct, and not expired (5 minutes).
     * - Resets the forgotPasswordCount and failed login attempts upon success.
     * 
     * @param request payload containing user identifier, OTP code, and new password
     * @return status message indicating successful reset or validation error
     */
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        boolean reset = authService.resetPassword(request);
        if (reset) {
            return ResponseEntity.ok("Password reset successfully");
        }
        return ResponseEntity.badRequest().body("Failed to reset password");
    }
}
