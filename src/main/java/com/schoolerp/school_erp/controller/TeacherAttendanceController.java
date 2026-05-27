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

@RestController
@RequestMapping("/api/teacher/attendance")
public class TeacherAttendanceController {

    @Autowired
    private StudentAttendanceService attendanceService;

    @PostMapping("/mark")
    public ResponseEntity<AttendanceResponse> markAttendance(
            @Valid @RequestBody AttendanceMarkRequest request,
            @RequestHeader("X-User-ID") UUID markerUserId) { 
        
        AttendanceResponse response = attendanceService.markAttendance(request, markerUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/logs")
    public ResponseEntity<List<AttendanceResponse>> getSectionLogs(
            @RequestParam UUID classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        List<AttendanceResponse> logs = attendanceService.getSectionAttendanceLog(classId, date);
        return ResponseEntity.ok(logs);
    }
}
