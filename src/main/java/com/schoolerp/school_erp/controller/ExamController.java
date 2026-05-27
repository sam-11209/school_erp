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

/**
 * Controller to handle exam scheduling, mark entry, and retrieval of gradebooks/marks.
 * 
 * Constraints:
 * - Secured under "/api/exams/**", requiring a valid authentication token.
 * - Requires X-Tenant-ID header to resolve database tenant context.
 */
@RestController
@RequestMapping("/api/exams")
public class ExamController {

    @Autowired
    private ExamService examService;

    /**
     * Creates a new exam entry for a class subject.
     * 
     * Constraints:
     * - Requires standard JWT in Authorization header (Bearer token).
     * - Requires X-Tenant-ID header.
     * - Accessible only to roles: SUPER_ADMIN, ADMIN, PRINCIPAL, TEACHER.
     * - Request body must be valid and conform to ExamCreateRequest.
     * - The creator's user ID is extracted automatically from the security context (UserContext).
     * 
     * @param request payload specifying subject, class section, date, and max marks
     * @return the created exam detail response
     */
    @PostMapping("/create")
    @RequiresRoles({"SUPER_ADMIN", "ADMIN", "PRINCIPAL", "TEACHER"})
    public ResponseEntity<?> createExam(
            @Valid @RequestBody ExamCreateRequest request) {

        UUID creatorUserId = UserContext.getCurrentUser();
        ExamMarkResponse response = examService.createExam(request, creatorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Enters or updates exam marks for a specific student and exam.
     * 
     * Constraints:
     * - Requires standard JWT in Authorization header.
     * - Requires X-Tenant-ID header.
     * - Accessible only to roles: SUPER_ADMIN, ADMIN, PRINCIPAL, TEACHER.
     * - Request body must be valid and conform to ExamMarkRequest.
     * 
     * @param request payload specifying exam ID, student profile ID, and marks obtained
     * @return the updated exam mark details
     */
    @PostMapping("/marks")
    @RequiresRoles({"SUPER_ADMIN", "ADMIN", "PRINCIPAL", "TEACHER"})
    public ResponseEntity<?> enterMarks(
            @Valid @RequestBody ExamMarkRequest request) {

        ExamMarkResponse response = examService.enterMarks(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the entire gradebook (all exam marks) for a specific student.
     * 
     * Constraints:
     * - Requires standard JWT in Authorization header.
     * - Requires X-Tenant-ID header.
     * - Expects the student's profile ID as a query parameter.
     * 
     * @param studentProfileId the student's profile ID
     * @return response containing the list of exam marks and overall performance summaries
     */
    @GetMapping("/gradebook")
    public ResponseEntity<GradebookResponse> getGradebook(
            @RequestParam UUID studentProfileId) {

        GradebookResponse response = examService.getGradebook(studentProfileId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all recorded marks for a given exam.
     * 
     * Constraints:
     * - Requires standard JWT in Authorization header.
     * - Requires X-Tenant-ID header.
     * - Expects the exam ID as a path variable.
     * 
     * @param examId the ID of the exam
     * @return list of marks for all students who sat for this exam
     */
    @GetMapping("/{examId}/marks")
    public ResponseEntity<List<ExamMarkResponse>> getExamMarks(
            @PathVariable UUID examId) {

        List<ExamMarkResponse> response = examService.getExamMarks(examId);
        return ResponseEntity.ok(response);
    }
}
