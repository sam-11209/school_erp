package com.schoolerp.school_erp.service;

import com.schoolerp.school_erp.dto.StudentProfileResponse;
import java.util.UUID;

public interface StudentService {
    StudentProfileResponse getProfile(UUID studentId);
    boolean updateProfileStatus(UUID studentId, boolean isActive);
    boolean assignSection(UUID studentId, UUID sectionId);
}
