package com.schoolerp.school_erp.repository;

import com.schoolerp.school_erp.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findBySchoolIdAndNameAndDeletedAtIsNull(UUID schoolId, String name);
}
