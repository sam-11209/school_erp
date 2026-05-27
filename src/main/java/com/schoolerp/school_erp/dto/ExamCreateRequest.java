package com.schoolerp.school_erp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class ExamCreateRequest {
    @NotNull(message = "Exam Type ID is required")
    private UUID examTypeId;

    @NotNull(message = "Class ID is required")
    private UUID classId;

    private UUID sectionId;

    @NotNull(message = "Subject ID is required")
    private UUID subjectId;

    @NotNull(message = "Exam date is required")
    private LocalDate examDate;

    @NotNull(message = "Max marks is required")
    @Positive(message = "Max marks must be greater than zero")
    private BigDecimal maxMarks;
}
