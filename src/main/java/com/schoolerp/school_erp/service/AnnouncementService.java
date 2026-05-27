package com.schoolerp.school_erp.service;

import com.schoolerp.school_erp.dto.AnnouncementRequest;
import com.schoolerp.school_erp.dto.AnnouncementResponse;

import java.util.List;
import java.util.UUID;

public interface AnnouncementService {
    AnnouncementResponse createAnnouncement(AnnouncementRequest request, UUID creatorUserId);
    List<AnnouncementResponse> getAnnouncementsForUser(UUID userId);
    List<AnnouncementResponse> getSchoolAnnouncements();
}
