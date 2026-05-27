package com.schoolerp.school_erp.repository;

import com.schoolerp.school_erp.entity.Homework;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HomeworkRepository extends JpaRepository<Homework, UUID> {
    Optional<Homework> findByIdAndSchoolIdAndDeletedAtIsNull(UUID id, UUID schoolId);
    List<Homework> findBySchoolIdAndClassIdAndDeletedAtIsNull(UUID schoolId, UUID classId);
    List<Homework> findBySchoolIdAndClassIdAndSectionIdAndDeletedAtIsNull(UUID schoolId, UUID classId, UUID sectionId);
    List<Homework> findBySchoolIdAndDeletedAtIsNull(UUID schoolId);
}
