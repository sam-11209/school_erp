package com.schoolerp.school_erp.service;

import com.schoolerp.school_erp.dto.LoginRequest;
import com.schoolerp.school_erp.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    boolean verifyOTP(String email, String code);
    boolean forgotPassword(String email);
    boolean changePassword(String email, String oldPassword, String newPassword);
}
