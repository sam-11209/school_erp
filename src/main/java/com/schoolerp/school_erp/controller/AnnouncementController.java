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

/**
 * Controller to manage school announcements.
 * Supports creating school-wide or target-audience announcements,
 * and fetching announcements specific to the current user or globally.
 * 
 * Constraints:
 * - Secured under "/api/announcements/**", requiring a valid authentication token.
 * - Requires X-Tenant-ID header to resolve database tenant context.
 */
@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    /**
     * Creates and publishes a new announcement.
     * 
     * Constraints:
     * - Requires standard JWT in Authorization header (Bearer token).
     * - Requires X-Tenant-ID header.
     * - Accessible only to roles: SUPER_ADMIN, ADMIN, PRINCIPAL, TEACHER.
     * - Request body must be valid and conform to AnnouncementRequest.
     * - The creator's user ID is extracted automatically from the security context (UserContext).
     * 
     * @param request payload containing target audience, title, and content
     * @return the created announcement detail response
     */
    @PostMapping
    @RequiresRoles({"SUPER_ADMIN", "ADMIN", "PRINCIPAL", "TEACHER"})
    public ResponseEntity<?> createAnnouncement(
            @Valid @RequestBody AnnouncementRequest request) {

        UUID creatorUserId = UserContext.getCurrentUser();
        AnnouncementResponse response = announcementService.createAnnouncement(request, creatorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves all announcements relevant to the logged-in user.
     * Announcement visibility is determined by the user's roles and class associations.
     * 
     * Constraints:
     * - Requires standard JWT in Authorization header.
     * - Requires X-Tenant-ID header.
     * - The user's ID is extracted automatically from the security context (UserContext).
     * 
     * @return list of announcements targeting the authenticated user
     */
    @GetMapping
    public ResponseEntity<List<AnnouncementResponse>> getMyAnnouncements() {
        UUID userId = UserContext.getCurrentUser();
        List<AnnouncementResponse> response = announcementService.getAnnouncementsForUser(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all general announcements for the school (tenant).
     * 
     * Constraints:
     * - Requires standard JWT in Authorization header.
     * - Requires X-Tenant-ID header.
     * 
     * @return list of all school announcements
     */
    @GetMapping("/all")
    public ResponseEntity<List<AnnouncementResponse>> getAllAnnouncements() {
        List<AnnouncementResponse> response = announcementService.getSchoolAnnouncements();
        return ResponseEntity.ok(response);
    }
}
