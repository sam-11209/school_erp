package com.schoolerp.school_erp.service.impl;

import com.schoolerp.school_erp.dto.AnnouncementRequest;
import com.schoolerp.school_erp.dto.AnnouncementResponse;
import com.schoolerp.school_erp.entity.*;
import com.schoolerp.school_erp.filter.TenantContext;
import com.schoolerp.school_erp.repository.AnnouncementRepository;
import com.schoolerp.school_erp.repository.SchoolRepository;
import com.schoolerp.school_erp.repository.UserRepository;
import com.schoolerp.school_erp.service.AnnouncementService;
import com.schoolerp.school_erp.strategy.NotificationFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private NotificationFactory notificationFactory;

    @Override
    @Transactional
    public AnnouncementResponse createAnnouncement(AnnouncementRequest request, UUID creatorUserId) {
        UUID schoolId = TenantContext.getCurrentTenant();
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new IllegalArgumentException("School not found"));

        User creator = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Announcement announcement = Announcement.builder()
                .school(school)
                .title(request.getTitle())
                .content(request.getContent())
                .targetRole(request.getTargetRole() != null ? request.getTargetRole().toUpperCase() : "ALL")
                .classId(request.getClassId())
                .createdBy(creator)
                .build();

        announcement = announcementRepository.save(announcement);

        // Dispatch simulated notification
        try {
            String audience = announcement.getTargetRole() + 
                (announcement.getClassId() != null ? " (Class ID: " + announcement.getClassId() + ")" : "");
            String message = "New Announcement: " + announcement.getTitle() + " - " + announcement.getContent();
            notificationFactory.getService("whatsapp").sendNotice(audience, message);
        } catch (Exception e) {
            // Log warning but do not fail announcement creation
            org.slf4j.LoggerFactory.getLogger(AnnouncementServiceImpl.class)
                .warn("Failed to dispatch WhatsApp notification for announcement: {}", e.getMessage());
        }

        return mapToResponse(announcement);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getAnnouncementsForUser(UUID userId) {
        UUID schoolId = TenantContext.getCurrentTenant();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.getSchool().getId().equals(schoolId)) {
            throw new SecurityException("Unauthorized tenant access to user context.");
        }

        List<Announcement> announcements = announcementRepository.findBySchoolIdAndDeletedAtIsNull(schoolId);

        // Filter based on user roles
        return announcements.stream()
                .filter(ann -> {
                    if (ann.getTargetRole() == null || ann.getTargetRole().equalsIgnoreCase("ALL")) {
                        return true;
                    }
                    return user.getRoles().stream()
                            .anyMatch(role -> role.getName().equalsIgnoreCase(ann.getTargetRole()));
                })
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getSchoolAnnouncements() {
        UUID schoolId = TenantContext.getCurrentTenant();
        List<Announcement> announcements = announcementRepository.findBySchoolIdAndDeletedAtIsNull(schoolId);
        return announcements.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private AnnouncementResponse mapToResponse(Announcement announcement) {
        return AnnouncementResponse.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .content(announcement.getContent())
                .targetRole(announcement.getTargetRole())
                .classId(announcement.getClassId())
                .createdByUserId(announcement.getCreatedBy().getId())
                .createdByUserName(announcement.getCreatedBy().getEmail())
                .createdAt(announcement.getCreatedAt())
                .build();
    }
}
