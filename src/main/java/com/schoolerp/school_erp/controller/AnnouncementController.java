package com.schoolerp.school_erp.controller;

import com.schoolerp.school_erp.dto.AnnouncementRequest;
import com.schoolerp.school_erp.dto.AnnouncementResponse;
import com.schoolerp.school_erp.entity.User;
import com.schoolerp.school_erp.enums.RoleType;
import com.schoolerp.school_erp.repository.UserRepository;
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

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> createAnnouncement(
            @Valid @RequestBody AnnouncementRequest request,
            @RequestHeader("X-User-ID") UUID creatorUserId) {

        User user = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isAuthorized = user.getRoles().stream().anyMatch(role -> 
            role.getName().equalsIgnoreCase("SUPER_ADMIN") || 
            role.getName().equalsIgnoreCase("ADMIN") || 
            role.getName().equalsIgnoreCase("PRINCIPAL") || 
            role.getName().equalsIgnoreCase("TEACHER")
        );

        if (!isAuthorized) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Unauthorized: Only Teachers, Principals, or Admins can post announcements.");
        }

        AnnouncementResponse response = announcementService.createAnnouncement(request, creatorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AnnouncementResponse>> getMyAnnouncements(
            @RequestHeader("X-User-ID") UUID userId) {

        List<AnnouncementResponse> response = announcementService.getAnnouncementsForUser(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<AnnouncementResponse>> getAllAnnouncements() {
        List<AnnouncementResponse> response = announcementService.getSchoolAnnouncements();
        return ResponseEntity.ok(response);
    }
}
