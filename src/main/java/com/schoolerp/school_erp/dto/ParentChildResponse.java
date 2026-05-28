package com.schoolerp.school_erp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParentChildResponse {
    private UUID studentId;
    private String fullName;
    private String email;
    private String mobileNo;
    private String admissionNumber;
    private String rollNumber;
    private UUID classId;
    private String className;
    private UUID sectionId;
    private String sectionName;
}
