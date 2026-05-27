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
public class StudentProfileResponse {
    private UUID id;
    private String admissionNumber;
    private String rollNumber;
    private UUID classId;
    private UUID sectionId;
    private String email;
    private String firstName;
    private String lastName;
    private Boolean isActive;
}
