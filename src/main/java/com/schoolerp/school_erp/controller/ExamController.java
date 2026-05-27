package com.schoolerp.school_erp.controller;

import com.schoolerp.school_erp.dto.ExamCreateRequest;
import com.schoolerp.school_erp.dto.ExamMarkRequest;
import com.schoolerp.school_erp.dto.ExamMarkResponse;
import com.schoolerp.school_erp.dto.GradebookResponse;
import com.schoolerp.school_erp.security.RequiresRoles;
import com.schoolerp.school_erp.security.UserContext;
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

    @PostMapping("/create")
    @RequiresRoles({"SUPER_ADMIN", "ADMIN", "PRINCIPAL", "TEACHER"})
    public ResponseEntity<?> createExam(
            @Valid @RequestBody ExamCreateRequest request) {

        UUID creatorUserId = UserContext.getCurrentUser();
        ExamMarkResponse response = examService.createExam(request, creatorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/marks")
    @RequiresRoles({"SUPER_ADMIN", "ADMIN", "PRINCIPAL", "TEACHER"})
    public ResponseEntity<?> enterMarks(
            @Valid @RequestBody ExamMarkRequest request) {

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
