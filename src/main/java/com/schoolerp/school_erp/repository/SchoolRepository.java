package com.schoolerp.school_erp.repository;

import com.schoolerp.school_erp.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolRepository extends JpaRepository<School, UUID> {
    Optional<School> findBySubdomainAndDeletedAtIsNull(String subdomain);
}
