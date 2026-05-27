package com.schoolerp.school_erp.controller;

import com.schoolerp.school_erp.dto.HomeworkCreateRequest;
import com.schoolerp.school_erp.dto.HomeworkResponse;
import com.schoolerp.school_erp.dto.HomeworkSubmissionRequest;
import com.schoolerp.school_erp.entity.HomeworkSubmission;
import com.schoolerp.school_erp.entity.User;
import com.schoolerp.school_erp.enums.RoleType;
import com.schoolerp.school_erp.repository.UserRepository;
import com.schoolerp.school_erp.service.HomeworkService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/homework")
public class HomeworkController {

    @Autowired
    private HomeworkService homeworkService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> createHomework(
            @Valid @RequestBody HomeworkCreateRequest request,
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
                    .body("Unauthorized: Only Teachers, Principals, or Admins can post homework.");
        }

        HomeworkResponse response = homeworkService.createHomework(request, creatorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<?> submitHomework(
            @PathVariable("id") UUID id,
            @Valid @RequestBody HomeworkSubmissionRequest request,
            @RequestHeader("X-User-ID") UUID studentUserId) {

        request.setHomeworkId(id);

        User user = userRepository.findById(studentUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isAuthorized = user.getRoles().stream().anyMatch(role -> 
            role.getName().equalsIgnoreCase("SUPER_ADMIN") || 
            role.getName().equalsIgnoreCase("ADMIN") || 
            role.getName().equalsIgnoreCase("STUDENT")
        );

        if (!isAuthorized) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Unauthorized: Only Students or Admins can submit homework.");
        }

        HomeworkSubmission response = homeworkService.submitHomework(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/submission/{submissionId}/grade")
    public ResponseEntity<?> gradeSubmission(
            @PathVariable UUID submissionId,
            @RequestParam BigDecimal marks,
            @RequestParam String remarks,
            @RequestHeader("X-User-ID") UUID teacherUserId) {

        User user = userRepository.findById(teacherUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isAuthorized = user.getRoles().stream().anyMatch(role -> 
            role.getName().equalsIgnoreCase("SUPER_ADMIN") || 
            role.getName().equalsIgnoreCase("ADMIN") || 
            role.getName().equalsIgnoreCase("PRINCIPAL") || 
            role.getName().equalsIgnoreCase("TEACHER")
        );

        if (!isAuthorized) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Unauthorized: Only Teachers, Principals, or Admins can grade submissions.");
        }

        HomeworkSubmission response = homeworkService.gradeSubmission(submissionId, marks, remarks, teacherUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<List<HomeworkResponse>> getHomeworksForClass(@PathVariable UUID classId) {
        List<HomeworkResponse> response = homeworkService.getHomeworksForClass(classId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/submission/list")
    public ResponseEntity<List<HomeworkSubmission>> getSubmissions(@RequestParam UUID homeworkId) {
        List<HomeworkSubmission> response = homeworkService.getSubmissionsForHomework(homeworkId);
        return ResponseEntity.ok(response);
    }
}
