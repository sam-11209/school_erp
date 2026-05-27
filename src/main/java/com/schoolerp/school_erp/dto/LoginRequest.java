package com.schoolerp.school_erp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Mobile number is required")
    private String mobileNo;

    @NotBlank(message = "Password is required")
    private String password;
}
