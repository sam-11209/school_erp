package com.schoolerp.school_erp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequest {
    private String emailId;
    private String mobileNo;

    @NotBlank(message = "OTP code is required")
    private String code;

    @NotBlank(message = "New password is required")
    private String newPassword;
}
