package com.schoolerp.school_erp.controller;

import com.schoolerp.school_erp.dto.LoginRequest;
import com.schoolerp.school_erp.dto.LoginResponse;
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
     * Authenticates a user by email and password under a specific tenant.
     * If MFA is enabled, triggers an OTP send to the user's mobile number (or email) and returns
     * an "MFA_REQUIRED" status without generating a session token.
     * 
     * Constraints:
     * - Requires the X-Tenant-ID header to resolve the school context.
     * - Blocks MFA login if the user has already logged in with an OTP (otp_used is true).
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Generates and sends a new 6-digit OTP code to the provided mobile number via WhatsApp.
     * 
     * Constraints:
     * - Requires the X-Tenant-ID header.
     * - The mobile number must belong to a registered user within the tenant school.
     * - Blocks requests if the user has already logged in with an OTP (otp_used is true).
     * - Resets the OTP resend count to 0.
     */
    @PostMapping("/send-otp")
    public ResponseEntity<String> sendOTP(@RequestParam String mobileNo) {
        boolean sent = authService.sendOTP(mobileNo);
        if (sent) {
            return ResponseEntity.ok("OTP sent successfully to WhatsApp");
        }
        return ResponseEntity.badRequest().body("Failed to send OTP");
    }

    /**
     * Resends a new 6-digit OTP code for the active OTP session to the specified mobile number.
     * 
     * Constraints:
     * - Requires the X-Tenant-ID header.
     * - Must have an active, initialized OTP session.
     * - Maximum 3 resend attempts allowed per session (resend count is tracked).
     * - Blocks requests if the user has already logged in with an OTP (otp_used is true).
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<String> resendOTP(@RequestParam String mobileNo) {
        boolean sent = authService.resendOTP(mobileNo);
        if (sent) {
            return ResponseEntity.ok("OTP resent successfully to WhatsApp");
        }
        return ResponseEntity.badRequest().body("Failed to resend OTP");
    }

    /**
     * Verifies the provided OTP code against the database record for the user's email.
     * 
     * Constraints:
     * - Requires the X-Tenant-ID header.
     * - Checks if the stored OTP exists, matches the input code, and is not expired (expires in 5 minutes).
     * - Blocks requests if the user has already logged in with an OTP (otp_used is true).
     * - On successful verification, clears the OTP fields and marks otp_used as true (preventing future OTP logins).
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOTP(@RequestParam String email, @RequestParam String code) {
        boolean isValid = authService.verifyOTP(email, code);
        if (isValid) {
            return ResponseEntity.ok("OTP verified successfully");
        }
        return ResponseEntity.badRequest().body("Invalid or expired OTP");
    }

    /**
     * Generates and sends a password reset OTP to the user's WhatsApp.
     * 
     * Constraints:
     * - Requires the X-Tenant-ID header.
     * - The email address must belong to a registered user in the tenant school.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam String email) {
        boolean sent = authService.forgotPassword(email);
        if (sent) {
            return ResponseEntity.ok("Password reset OTP sent to WhatsApp");
        }
        return ResponseEntity.badRequest().body("Failed to send reset link");
    }
}
