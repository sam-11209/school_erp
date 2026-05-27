package com.schoolerp.school_erp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ExamMarkRequest {
    @NotNull(message = "Exam ID is required")
    private UUID examId;

    @NotNull(message = "Student Profile ID is required")
    private UUID studentProfileId;

    @NotNull(message = "Marks obtained is required")
    @Min(value = 0, message = "Marks obtained cannot be negative")
    private BigDecimal marksObtained;

    private String remarks;
}
