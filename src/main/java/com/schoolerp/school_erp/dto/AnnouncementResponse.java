package com.schoolerp.school_erp.dto;

import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class AnnouncementResponse {
    private UUID id;
    private String title;
    private String content;
    private String targetRole;
    private UUID classId;
    private UUID createdByUserId;
    private String createdByUserName;
    private OffsetDateTime createdAt;
}
