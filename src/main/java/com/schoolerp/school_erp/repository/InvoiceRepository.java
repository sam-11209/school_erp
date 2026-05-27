package com.schoolerp.school_erp.repository;

import com.schoolerp.school_erp.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    java.util.Optional<Invoice> findByIdAndSchoolIdAndDeletedAtIsNull(UUID id, UUID schoolId);
    List<Invoice> findBySchoolIdAndStudentIdAndDeletedAtIsNull(UUID schoolId, UUID studentId);
}
