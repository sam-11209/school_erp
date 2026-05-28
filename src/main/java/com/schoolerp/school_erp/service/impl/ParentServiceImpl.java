package com.schoolerp.school_erp.service.impl;

import com.schoolerp.school_erp.dto.ParentChildResponse;
import com.schoolerp.school_erp.entity.ParentStudentMapping;
import com.schoolerp.school_erp.entity.StudentProfile;
import com.schoolerp.school_erp.entity.User;
import com.schoolerp.school_erp.filter.TenantContext;
import com.schoolerp.school_erp.repository.*;
import com.schoolerp.school_erp.service.ParentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ParentServiceImpl implements ParentService {

    private static final Logger log = LoggerFactory.getLogger(ParentServiceImpl.class);

    @Autowired
    private ParentStudentMappingRepository parentStudentMappingRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ParentChildResponse> getChildren(UUID parentUserId) {
        UUID schoolId = TenantContext.getCurrentTenant();
        if (schoolId == null) {
            throw new IllegalStateException("Tenant School ID must be provided in request header.");
        }
        log.info("Fetching children for parent user: {} under school tenant: {}", parentUserId, schoolId);

        List<ParentStudentMapping> mappings = parentStudentMappingRepository.findByParentId(parentUserId);
        List<ParentChildResponse> responseList = new ArrayList<>();

        for (ParentStudentMapping mapping : mappings) {
            User studentUser = mapping.getStudent();
            // Verify school tenant constraint
            if (studentUser.getSchool() != null && !studentUser.getSchool().getId().equals(schoolId)) {
                log.warn("Child user {} belongs to school {}, but parent request was under tenant {}",
                        studentUser.getId(), studentUser.getSchool().getId(), schoolId);
                continue;
            }

            Optional<StudentProfile> profileOpt = studentProfileRepository.findByUserIdAndDeletedAtIsNull(studentUser.getId());

            ParentChildResponse.ParentChildResponseBuilder builder = ParentChildResponse.builder()
                    .studentId(studentUser.getId())
                    .fullName(studentUser.getFullName())
                    .email(studentUser.getEmail())
                    .mobileNo(studentUser.getMobileNo());

            if (profileOpt.isPresent()) {
                StudentProfile profile = profileOpt.get();
                builder.admissionNumber(profile.getAdmissionNumber())
                        .rollNumber(profile.getRollNumber())
                        .classId(profile.getClassId())
                        .sectionId(profile.getSectionId());

                // Fetch Class Name
                if (profile.getClassId() != null) {
                    schoolClassRepository.findById(profile.getClassId())
                            .ifPresent(sc -> builder.className(sc.getName()));
                }

                // Fetch Section Name
                if (profile.getSectionId() != null) {
                    sectionRepository.findById(profile.getSectionId())
                            .ifPresent(sec -> builder.sectionName(sec.getName()));
                }
            }

            responseList.add(builder.build());
        }

        return responseList;
    }
}
