package com.schoolerp.school_erp.repository;

import com.schoolerp.school_erp.entity.ExamType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamTypeRepository extends JpaRepository<ExamType, UUID> {
    Optional<ExamType> findByIdAndSchoolIdAndDeletedAtIsNull(UUID id, UUID schoolId);
    Optional<ExamType> findByCodeAndSchoolIdAndDeletedAtIsNull(String code, UUID schoolId);
    List<ExamType> findBySchoolIdAndDeletedAtIsNull(UUID schoolId);
}
