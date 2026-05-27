package com.schoolerp.school_erp.controller;

import com.schoolerp.school_erp.dto.AttendanceMarkRequest;
import com.schoolerp.school_erp.dto.AttendanceResponse;
import com.schoolerp.school_erp.service.StudentAttendanceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Controller to handle teacher action logs for student attendance.
 * Allows marking student attendance and viewing historic logs per class section.
 * 
 * Constraints:
 * - Secured under "/api/teacher/attendance/**", requiring a valid authentication token.
 * - Requires X-Tenant-ID header to resolve database tenant context.
 */
@RestController
@RequestMapping("/api/teacher/attendance")
public class TeacherAttendanceController {

    @Autowired
    private StudentAttendanceService attendanceService;

    /**
     * Marks student attendance for a particular section on a given day.
     * 
     * Constraints:
     * - Requires standard JWT in Authorization header.
     * - Requires X-Tenant-ID header.
     * - Requires X-User-ID header to specify the teacher/admin performing the marking action.
     * - Request body must be valid and conform to AttendanceMarkRequest.
     * 
     * @param request payload specifying class section, date, and attendance list of students
     * @param markerUserId the ID of the user (e.g. teacher) submitting the attendance record
     * @return the marked attendance summary details
     */
    @PostMapping("/mark")
    public ResponseEntity<AttendanceResponse> markAttendance(
            @Valid @RequestBody AttendanceMarkRequest request,
            @RequestHeader("X-User-ID") UUID markerUserId) { 
        
        AttendanceResponse response = attendanceService.markAttendance(request, markerUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the list of student attendance records for a specific section and date.
     * 
     * Constraints:
     * - Requires standard JWT in Authorization header.
     * - Requires X-Tenant-ID header.
     * - Expects classId and date (in ISO date format YYYY-MM-DD) as request parameters.
     * 
     * @param classId the class section ID
     * @param date the target date for the logs
     * @return list of attendance details for the specified class section and date
     */
    @GetMapping("/logs")
    public ResponseEntity<List<AttendanceResponse>> getSectionLogs(
            @RequestParam UUID classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        List<AttendanceResponse> logs = attendanceService.getSectionAttendanceLog(classId, date);
        return ResponseEntity.ok(logs);
    }
}
