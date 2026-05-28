package com.schoolerp.school_erp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendOtpRequest {
    private String mobileNo;
    private boolean sendSms;
    private String emailId;
    private boolean sendEmail;
}
