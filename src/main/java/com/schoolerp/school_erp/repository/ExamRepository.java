package com.schoolerp.school_erp.repository;

import com.schoolerp.school_erp.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamRepository extends JpaRepository<Exam, UUID> {
    Optional<Exam> findByIdAndSchoolIdAndDeletedAtIsNull(UUID id, UUID schoolId);
    List<Exam> findBySchoolIdAndClassIdAndDeletedAtIsNull(UUID schoolId, UUID classId);
    List<Exam> findBySchoolIdAndClassIdAndSectionIdAndDeletedAtIsNull(UUID schoolId, UUID classId, UUID sectionId);
    List<Exam> findBySchoolIdAndDeletedAtIsNull(UUID schoolId);
}
