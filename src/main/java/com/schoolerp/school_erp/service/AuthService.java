package com.schoolerp.school_erp.service;

import com.schoolerp.school_erp.dto.*;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    boolean verifyOTP(VerifyOtpRequest request);
    SendOtpResponse sendOTP(SendOtpRequest request);
    SendOtpResponse resendOTP(ResendOtpRequest request);
    boolean forgotPassword(String email, String mobileNo);
    boolean changePassword(String email, String oldPassword, String newPassword);
}
