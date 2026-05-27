package com.schoolerp.school_erp.repository;

import com.schoolerp.school_erp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findBySchoolIdAndEmailAndDeletedAtIsNull(UUID schoolId, String email);
    Optional<User> findBySchoolIdAndMobileNoAndDeletedAtIsNull(UUID schoolId, String mobileNo);
    Optional<User> findBySchoolIdAndMobileNoAndOtpUsedFalseAndDeletedAtIsNull(UUID schoolId, String mobileNo);
}
