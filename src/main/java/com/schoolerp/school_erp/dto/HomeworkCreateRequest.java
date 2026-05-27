package com.schoolerp.school_erp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class HomeworkCreateRequest {
    @NotNull(message = "Class ID is required")
    private UUID classId;

    private UUID sectionId;

    @NotNull(message = "Subject ID is required")
    private UUID subjectId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Due date is required")
    private OffsetDateTime dueDate;

    private String fileAttachmentPath;
}
