package com.schoolerp.school_erp.controller;

import com.schoolerp.school_erp.dto.AnnouncementRequest;
import com.schoolerp.school_erp.dto.AnnouncementResponse;
import com.schoolerp.school_erp.security.RequiresRoles;
import com.schoolerp.school_erp.security.UserContext;
import com.schoolerp.school_erp.service.AnnouncementService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @PostMapping
    @RequiresRoles({"SUPER_ADMIN", "ADMIN", "PRINCIPAL", "TEACHER"})
    public ResponseEntity<?> createAnnouncement(
            @Valid @RequestBody AnnouncementRequest request) {

        UUID creatorUserId = UserContext.getCurrentUser();
        AnnouncementResponse response = announcementService.createAnnouncement(request, creatorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AnnouncementResponse>> getMyAnnouncements() {
        UUID userId = UserContext.getCurrentUser();
        List<AnnouncementResponse> response = announcementService.getAnnouncementsForUser(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<AnnouncementResponse>> getAllAnnouncements() {
        List<AnnouncementResponse> response = announcementService.getSchoolAnnouncements();
        return ResponseEntity.ok(response);
    }
}
