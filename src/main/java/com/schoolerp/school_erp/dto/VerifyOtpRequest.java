package com.schoolerp.school_erp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpRequest {
    private String otpId;
    private String mobileNo;
    private String emailId;
    private String code;
}
