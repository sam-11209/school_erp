package com.schoolerp.school_erp.repository;

import com.schoolerp.school_erp.entity.HomeworkSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HomeworkSubmissionRepository extends JpaRepository<HomeworkSubmission, UUID> {
    Optional<HomeworkSubmission> findByIdAndSchoolIdAndDeletedAtIsNull(UUID id, UUID schoolId);
    Optional<HomeworkSubmission> findByHomeworkIdAndStudentIdAndDeletedAtIsNull(UUID homeworkId, UUID studentProfileId);
    List<HomeworkSubmission> findByHomeworkIdAndSchoolIdAndDeletedAtIsNull(UUID homeworkId, UUID schoolId);
    List<HomeworkSubmission> findByStudentIdAndSchoolIdAndDeletedAtIsNull(UUID studentProfileId, UUID schoolId);
}
