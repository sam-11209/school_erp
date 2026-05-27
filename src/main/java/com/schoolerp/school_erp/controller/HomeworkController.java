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

/**
 * Controller to manage homework assignments, student submissions, and grading.
 * 
 * Constraints:
 * - Secured under "/api/homework/**", requiring a valid authentication token.
 * - Requires X-Tenant-ID header to resolve database tenant context.
 */
@RestController
@RequestMapping("/api/homework")
public class HomeworkController {

    @Autowired
    private HomeworkService homeworkService;

    /**
     * Creates a new homework assignment for a given class section.
     * 
     * Constraints:
     * - Requires standard JWT in Authorization header (Bearer token).
     * - Requires X-Tenant-ID header.
     * - Accessible only to roles: SUPER_ADMIN, ADMIN, PRINCIPAL, TEACHER.
     * - Request body must be valid and conform to HomeworkCreateRequest.
     * - Creator ID is automatically retrieved from the security context (UserContext).
     * 
     * @param request payload specifying details of the homework assignment
     * @return the created homework details
     */
    @PostMapping
    @RequiresRoles({"SUPER_ADMIN", "ADMIN", "PRINCIPAL", "TEACHER"})
    public ResponseEntity<?> createHomework(
            @Valid @RequestBody HomeworkCreateRequest request) {

        UUID creatorUserId = UserContext.getCurrentUser();
        HomeworkResponse response = homeworkService.createHomework(request, creatorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Submits a student's response/solution for a homework assignment.
     * 
     * Constraints:
     * - Requires standard JWT in Authorization header.
     * - Requires X-Tenant-ID header.
     * - Accessible only to roles: SUPER_ADMIN, ADMIN, STUDENT.
     * - Request body must be valid and conform to HomeworkSubmissionRequest.
     * - Takes the homework assignment ID from the path variable id.
     * 
     * @param id the homework ID
     * @param request submission payload containing answer files/content and student ID
     * @return the recorded homework submission details
     */
    @PostMapping("/{id}/submit")
    @RequiresRoles({"SUPER_ADMIN", "ADMIN", "STUDENT"})
    public ResponseEntity<?> submitHomework(
            @PathVariable("id") UUID id,
            @Valid @RequestBody HomeworkSubmissionRequest request) {

        request.setHomeworkId(id);
        HomeworkSubmission response = homeworkService.submitHomework(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Grades a submitted homework assignment with marks and feedback remarks.
     * 
     * Constraints:
     * - Requires standard JWT in Authorization header.
     * - Requires X-Tenant-ID header.
     * - Accessible only to roles: SUPER_ADMIN, ADMIN, PRINCIPAL, TEACHER.
     * - Expects marks and remarks as query parameters.
     * - Grading teacher ID is automatically retrieved from the security context (UserContext).
     * 
     * @param submissionId the ID of the submission to grade
     * @param marks the score or marks awarded
     * @param remarks qualitative feedback comments
     * @return the updated homework submission details with grading information
     */
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

    /**
     * Retrieves all homework assignments published for a specific class ID.
     * 
     * Constraints:
     * - Requires standard JWT in Authorization header.
     * - Requires X-Tenant-ID header.
     * - Expects class ID as a path variable.
     * 
     * @param classId the ID of the class
     * @return list of homework assignments associated with the class
     */
    @GetMapping("/class/{classId}")
    public ResponseEntity<List<HomeworkResponse>> getHomeworksForClass(@PathVariable UUID classId) {
        List<HomeworkResponse> response = homeworkService.getHomeworksForClass(classId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all student submissions for a specific homework assignment.
     * 
     * Constraints:
     * - Requires standard JWT in Authorization header.
     * - Requires X-Tenant-ID header.
     * - Expects homework ID as a query parameter.
     * 
     * @param homeworkId the ID of the homework assignment
     * @return list of student submissions for this assignment
     */
    @GetMapping("/submission/list")
    public ResponseEntity<List<HomeworkSubmission>> getSubmissions(@RequestParam UUID homeworkId) {
        List<HomeworkSubmission> response = homeworkService.getSubmissionsForHomework(homeworkId);
        return ResponseEntity.ok(response);
    }
}
