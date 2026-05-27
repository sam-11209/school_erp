package com.schoolerp.school_erp.repository;

import com.schoolerp.school_erp.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {
    Optional<Announcement> findByIdAndSchoolIdAndDeletedAtIsNull(UUID id, UUID schoolId);
    List<Announcement> findBySchoolIdAndDeletedAtIsNull(UUID schoolId);
    List<Announcement> findBySchoolIdAndTargetRoleOrTargetRoleAndDeletedAtIsNull(UUID schoolId, String targetRole, String targetRoleAll);
}
