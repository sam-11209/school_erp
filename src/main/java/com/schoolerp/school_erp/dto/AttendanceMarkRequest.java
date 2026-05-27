package com.schoolerp.school_erp.dto;

import com.schoolerp.school_erp.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class AttendanceMarkRequest {
    @NotNull(message = "Student ID is required")
    private UUID studentId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Status is required")
    private AttendanceStatus status;

    private String remarks;
    private UUID sectionSubjectId; 
}
