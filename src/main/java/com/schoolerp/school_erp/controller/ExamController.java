package com.schoolerp.school_erp.controller;

import com.schoolerp.school_erp.dto.ExamCreateRequest;
import com.schoolerp.school_erp.dto.ExamMarkRequest;
import com.schoolerp.school_erp.dto.ExamMarkResponse;
import com.schoolerp.school_erp.dto.GradebookResponse;
import com.schoolerp.school_erp.entity.User;
import com.schoolerp.school_erp.enums.RoleType;
import com.schoolerp.school_erp.repository.UserRepository;
import com.schoolerp.school_erp.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    @Autowired
    private ExamService examService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/create")
    public ResponseEntity<?> createExam(
            @Valid @RequestBody ExamCreateRequest request,
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
                    .body("Unauthorized: Only Teachers, Principals, or Admins can create tests/exams.");
        }

        ExamMarkResponse response = examService.createExam(request, creatorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/marks")
    public ResponseEntity<?> enterMarks(
            @Valid @RequestBody ExamMarkRequest request,
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
                    .body("Unauthorized: Only Teachers, Principals, or Admins can enter student marks.");
        }

        ExamMarkResponse response = examService.enterMarks(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/gradebook")
    public ResponseEntity<GradebookResponse> getGradebook(
            @RequestParam UUID studentProfileId) {

        GradebookResponse response = examService.getGradebook(studentProfileId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{examId}/marks")
    public ResponseEntity<List<ExamMarkResponse>> getExamMarks(
            @PathVariable UUID examId) {

        List<ExamMarkResponse> response = examService.getExamMarks(examId);
        return ResponseEntity.ok(response);
    }
}
