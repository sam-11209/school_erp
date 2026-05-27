package com.schoolerp.school_erp.repository;

import com.schoolerp.school_erp.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> {
    Optional<StudentProfile> findByUserIdAndDeletedAtIsNull(UUID userId);
}
