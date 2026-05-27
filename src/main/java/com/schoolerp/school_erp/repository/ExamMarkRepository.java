package com.schoolerp.school_erp.repository;

import com.schoolerp.school_erp.entity.ExamMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamMarkRepository extends JpaRepository<ExamMark, UUID> {
    Optional<ExamMark> findByIdAndSchoolIdAndDeletedAtIsNull(UUID id, UUID schoolId);
    Optional<ExamMark> findByExamIdAndStudentIdAndDeletedAtIsNull(UUID examId, UUID studentProfileId);
    List<ExamMark> findByExamIdAndSchoolIdAndDeletedAtIsNull(UUID examId, UUID schoolId);
    List<ExamMark> findByStudentIdAndSchoolIdAndDeletedAtIsNull(UUID studentProfileId, UUID schoolId);
}
