package com.schoolerp.school_erp.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class ExamMarkResponse {
    private UUID id;
    private UUID examId;
    private String examName;
    private UUID studentProfileId;
    private String studentName;
    private BigDecimal marksObtained;
    private BigDecimal maxMarks;
    private BigDecimal percentage;
    private String grade;
    private String remarks;
}
