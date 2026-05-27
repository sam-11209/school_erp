package com.schoolerp.school_erp.service;

import com.schoolerp.school_erp.dto.AttendanceMarkRequest;
import com.schoolerp.school_erp.dto.AttendanceResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface StudentAttendanceService {
    AttendanceResponse markAttendance(AttendanceMarkRequest request, UUID markerUserId);
    List<AttendanceResponse> getSectionAttendanceLog(UUID classId, LocalDate date);
}
