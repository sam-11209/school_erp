package com.schoolerp.school_erp.service.impl;

import com.schoolerp.school_erp.dto.StudentProfileResponse;
import com.schoolerp.school_erp.entity.StudentProfile;
import com.schoolerp.school_erp.filter.TenantContext;
import com.schoolerp.school_erp.repository.StudentProfileRepository;
import com.schoolerp.school_erp.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Override
    @Transactional(readOnly = true)
    public StudentProfileResponse getProfile(UUID studentId) {
        UUID schoolId = TenantContext.getCurrentTenant();
        StudentProfile profile = studentProfileRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        if (!profile.getUser().getSchool().getId().equals(schoolId)) {
            throw new SecurityException("Unauthorized tenant access to student profile.");
        }

        return mapToResponse(profile);
    }

    @Override
    @Transactional
    public boolean updateProfileStatus(UUID studentId, boolean isActive) {
        UUID schoolId = TenantContext.getCurrentTenant();
        StudentProfile profile = studentProfileRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        if (!profile.getUser().getSchool().getId().equals(schoolId)) {
            throw new SecurityException("Unauthorized tenant access to student profile.");
        }

        profile.setIsActive(isActive);
        studentProfileRepository.save(profile);
        return true;
    }

    @Override
    @Transactional
    public boolean assignSection(UUID studentId, UUID sectionId) {
        UUID schoolId = TenantContext.getCurrentTenant();
        StudentProfile profile = studentProfileRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        if (!profile.getUser().getSchool().getId().equals(schoolId)) {
            throw new SecurityException("Unauthorized tenant access to student profile.");
        }

        profile.setSectionId(sectionId);
        studentProfileRepository.save(profile);
        return true;
    }

    private StudentProfileResponse mapToResponse(StudentProfile profile) {
        return StudentProfileResponse.builder()
                .id(profile.getId())
                .admissionNumber(profile.getAdmissionNumber())
                .rollNumber(profile.getRollNumber())
                .classId(profile.getClassId())
                .sectionId(profile.getSectionId())
                .email(profile.getUser().getEmail())
                .firstName(profile.getUser().getId().toString()) // placeholder names mapping user profile later
                .lastName("")
                .isActive(profile.getIsActive())
                .build();
    }
}
