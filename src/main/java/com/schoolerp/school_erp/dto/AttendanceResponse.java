package com.schoolerp.school_erp.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class AttendanceResponse {
    private UUID id;
    private UUID studentId;
    private LocalDate date;
    private String status;
    private String remarks;
    private UUID sectionSubjectId;
    private String markedByName;
}
