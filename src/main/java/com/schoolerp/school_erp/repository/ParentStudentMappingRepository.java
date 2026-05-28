package com.schoolerp.school_erp.repository;

import com.schoolerp.school_erp.entity.ParentStudentMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ParentStudentMappingRepository extends JpaRepository<ParentStudentMapping, UUID> {
    List<ParentStudentMapping> findByParentId(UUID parentId);
    List<ParentStudentMapping> findByStudentId(UUID studentId);
}
