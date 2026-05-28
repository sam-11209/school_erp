package com.schoolerp.school_erp.repository;

import com.schoolerp.school_erp.entity.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface SchoolClassRepository extends JpaRepository<SchoolClass, UUID> {
    List<SchoolClass> findBySchoolIdAndDeletedAtIsNull(UUID schoolId);
}
