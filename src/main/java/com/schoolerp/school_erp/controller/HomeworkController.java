package com.schoolerp.school_erp.controller;

import com.schoolerp.school_erp.dto.HomeworkCreateRequest;
import com.schoolerp.school_erp.dto.HomeworkResponse;
import com.schoolerp.school_erp.dto.HomeworkSubmissionRequest;
import com.schoolerp.school_erp.entity.HomeworkSubmission;
import com.schoolerp.school_erp.security.RequiresRoles;
import com.schoolerp.school_erp.security.UserContext;
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

    @PostMapping
    @RequiresRoles({"SUPER_ADMIN", "ADMIN", "PRINCIPAL", "TEACHER"})
    public ResponseEntity<?> createHomework(
            @Valid @RequestBody HomeworkCreateRequest request) {

        UUID creatorUserId = UserContext.getCurrentUser();
        HomeworkResponse response = homeworkService.createHomework(request, creatorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/submit")
    @RequiresRoles({"SUPER_ADMIN", "ADMIN", "STUDENT"})
    public ResponseEntity<?> submitHomework(
            @PathVariable("id") UUID id,
            @Valid @RequestBody HomeworkSubmissionRequest request) {

        request.setHomeworkId(id);
        HomeworkSubmission response = homeworkService.submitHomework(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/submission/{submissionId}/grade")
    @RequiresRoles({"SUPER_ADMIN", "ADMIN", "PRINCIPAL", "TEACHER"})
    public ResponseEntity<?> gradeSubmission(
            @PathVariable UUID submissionId,
            @RequestParam BigDecimal marks,
            @RequestParam String remarks) {

        UUID teacherUserId = UserContext.getCurrentUser();
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
