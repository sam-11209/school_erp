package com.schoolerp.school_erp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResendOtpRequest {
    private String otpId;
    private String mobileNo;
    private boolean sendSms;
    private String emailId;
    private boolean sendEmail;

    public ResendOtpRequest(String otpId, String mobileNo, String emailId) {
        this.otpId = otpId;
        this.mobileNo = mobileNo;
        this.emailId = emailId;
    }
}
