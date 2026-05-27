package com.schoolerp.school_erp.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class GradebookResponse {
    private UUID studentProfileId;
    private String studentName;
    private String admissionNumber;
    private String classId;
    private String sectionId;
    private List<ExamMarkResponse> marks;
    private BigDecimal aggregatePercentage;
    private String overallGrade;
}
