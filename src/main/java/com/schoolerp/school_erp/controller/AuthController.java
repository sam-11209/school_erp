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

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/send-otp")
    public ResponseEntity<String> sendOTP(@RequestParam String mobileNo) {
        boolean sent = authService.sendOTP(mobileNo);
        if (sent) {
            return ResponseEntity.ok("OTP sent successfully to WhatsApp");
        }
        return ResponseEntity.badRequest().body("Failed to send OTP");
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<String> resendOTP(@RequestParam String mobileNo) {
        boolean sent = authService.resendOTP(mobileNo);
        if (sent) {
            return ResponseEntity.ok("OTP resent successfully to WhatsApp");
        }
        return ResponseEntity.badRequest().body("Failed to resend OTP");
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOTP(@RequestParam String email, @RequestParam String code) {
        boolean isValid = authService.verifyOTP(email, code);
        if (isValid) {
            return ResponseEntity.ok("OTP verified successfully");
        }
        return ResponseEntity.badRequest().body("Invalid or expired OTP");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam String email) {
        boolean sent = authService.forgotPassword(email);
        if (sent) {
            return ResponseEntity.ok("Password reset OTP sent to WhatsApp");
        }
        return ResponseEntity.badRequest().body("Failed to send reset link");
    }
}
