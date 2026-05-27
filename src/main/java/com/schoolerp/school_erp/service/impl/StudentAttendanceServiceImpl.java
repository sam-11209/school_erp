package com.schoolerp.school_erp.service.impl;

import com.schoolerp.school_erp.dto.AttendanceMarkRequest;
import com.schoolerp.school_erp.dto.AttendanceResponse;
import com.schoolerp.school_erp.entity.School;
import com.schoolerp.school_erp.entity.StudentAttendance;
import com.schoolerp.school_erp.entity.StudentProfile;
import com.schoolerp.school_erp.entity.User;
import com.schoolerp.school_erp.filter.TenantContext;
import com.schoolerp.school_erp.repository.SchoolRepository;
import com.schoolerp.school_erp.repository.StudentAttendanceRepository;
import com.schoolerp.school_erp.repository.StudentProfileRepository;
import com.schoolerp.school_erp.repository.UserRepository;
import com.schoolerp.school_erp.service.StudentAttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StudentAttendanceServiceImpl implements StudentAttendanceService {

    @Autowired
    private StudentAttendanceRepository attendanceRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Override
    @Transactional
    public AttendanceResponse markAttendance(AttendanceMarkRequest request, UUID markerUserId) {
        UUID schoolId = TenantContext.getCurrentTenant();
        if (schoolId == null) {
            throw new IllegalStateException("School Tenant ID not configured in header context.");
        }

        StudentProfile student = studentProfileRepository.findById(request.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + request.getStudentId()));

        User marker = userRepository.findById(markerUserId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher/Principal user not found."));

        Optional<StudentAttendance> existingRecord;
        if (request.getSectionSubjectId() == null) {
            existingRecord = attendanceRepository
                    .findByStudentIdAndDateAndSectionSubjectIdIsNullAndDeletedAtIsNull(request.getStudentId(), request.getDate());
        } else {
            existingRecord = attendanceRepository
                    .findByStudentIdAndDateAndSectionSubjectIdAndDeletedAtIsNull(request.getStudentId(), request.getDate(), request.getSectionSubjectId());
        }

        StudentAttendance record;
        if (existingRecord.isPresent()) {
            record = existingRecord.get();
            record.setStatus(request.getStatus());
            record.setRemarks(request.getRemarks());
            record.setMarkedBy(marker);
        } else {
            School school = schoolRepository.findById(schoolId)
                    .orElseThrow(() -> new IllegalStateException("Tenant School not found."));

            record = StudentAttendance.builder()
                    .school(school)
                    .student(student)
                    .date(request.getDate())
                    .status(request.getStatus())
                    .remarks(request.getRemarks())
                    .sectionSubjectId(request.getSectionSubjectId())
                    .markedBy(marker)
                    .build();
        }

        StudentAttendance saved = attendanceRepository.save(record);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getSectionAttendanceLog(UUID classId, LocalDate date) {
        UUID schoolId = TenantContext.getCurrentTenant();
        List<StudentAttendance> logs = attendanceRepository
                .findBySchoolIdAndStudentClassIdAndDateAndDeletedAtIsNull(schoolId, classId, date);
        
        return logs.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private AttendanceResponse mapToResponse(StudentAttendance attendance) {
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .studentId(attendance.getStudent().getId())
                .date(attendance.getDate())
                .status(attendance.getStatus().name())
                .remarks(attendance.getRemarks())
                .sectionSubjectId(attendance.getSectionSubjectId())
                .markedByName(attendance.getMarkedBy().getEmail()) 
                .build();
    }
}
