package com.schoolerp.school_erp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class HomeworkSubmissionRequest {
    @NotNull(message = "Homework ID is required")
    private UUID homeworkId;

    @NotNull(message = "Student Profile ID is required")
    private UUID studentProfileId;

    @NotBlank(message = "File path/link is required")
    private String filePath;
}
