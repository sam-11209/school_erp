package com.schoolerp.school_erp.dto;

import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class HomeworkResponse {
    private UUID id;
    private UUID classId;
    private UUID sectionId;
    private UUID subjectId;
    private String title;
    private String description;
    private OffsetDateTime dueDate;
    private String fileAttachmentPath;
    private UUID createdByUserId;
    private OffsetDateTime createdAt;
}
