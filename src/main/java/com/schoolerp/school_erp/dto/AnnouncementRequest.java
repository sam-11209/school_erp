package com.schoolerp.school_erp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.UUID;

@Data
public class AnnouncementRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    private String targetRole; // e.g. TEACHER, STUDENT, PARENT, ALL

    private UUID classId;
}
