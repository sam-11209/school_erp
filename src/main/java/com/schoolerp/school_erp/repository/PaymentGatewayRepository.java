package com.schoolerp.school_erp.repository;

import com.schoolerp.school_erp.entity.PaymentGateway;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentGatewayRepository extends JpaRepository<PaymentGateway, UUID> {
    Optional<PaymentGateway> findByIdAndSchoolIdAndDeletedAtIsNull(UUID id, UUID schoolId);
    Optional<PaymentGateway> findBySchoolIdAndProviderNameAndDeletedAtIsNull(UUID schoolId, String providerName);
    List<PaymentGateway> findBySchoolIdAndDeletedAtIsNull(UUID schoolId);
}
