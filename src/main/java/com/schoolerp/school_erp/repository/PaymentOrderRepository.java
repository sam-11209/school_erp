package com.schoolerp.school_erp.repository;

import com.schoolerp.school_erp.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, UUID> {
    Optional<PaymentOrder> findByIdAndSchoolIdAndDeletedAtIsNull(UUID id, UUID schoolId);
    Optional<PaymentOrder> findByGatewayOrderIdAndSchoolIdAndDeletedAtIsNull(String gatewayOrderId, UUID schoolId);
    List<PaymentOrder> findByInvoiceIdAndSchoolIdAndDeletedAtIsNull(UUID invoiceId, UUID schoolId);
}
