package com.schoolerp.school_erp.service.impl;

import com.schoolerp.school_erp.dto.InvoiceResponse;
import com.schoolerp.school_erp.entity.Invoice;
import com.schoolerp.school_erp.filter.TenantContext;
import com.schoolerp.school_erp.repository.InvoiceRepository;
import com.schoolerp.school_erp.service.BillingService;
import com.schoolerp.school_erp.strategy.LateFeeCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BillingServiceImpl implements BillingService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private LateFeeCalculator lateFeeCalculator;

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getStudentInvoices(UUID studentId) {
        UUID schoolId = TenantContext.getCurrentTenant();
        if (schoolId == null) {
            throw new IllegalStateException("School Tenant ID not found in context.");
        }

        List<Invoice> invoices = invoiceRepository.findBySchoolIdAndStudentIdAndDeletedAtIsNull(schoolId, studentId);
        return invoices.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InvoiceResponse calculateFeesAndLateFees(UUID invoiceId) {
        UUID schoolId = TenantContext.getCurrentTenant();
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        if (!invoice.getSchool().getId().equals(schoolId)) {
            throw new SecurityException("Unauthorized access to tenant invoice.");
        }

        // Fetch or default late fee strategy details
        String strategyType = "daily"; // Mapped from late_fee_policies configuration
        BigDecimal rate = BigDecimal.valueOf(10.00); // 10 INR per day

        BigDecimal lateFee = lateFeeCalculator.calculate(strategyType, invoice.getTotalAmount(), invoice.getDueDate(), rate);
        invoice.setLateFeeAmount(lateFee);
        invoice.recalculateBalance();

        Invoice saved = invoiceRepository.save(invoice);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public boolean recordPartialPayment(UUID invoiceId, BigDecimal amount, String method, String reference) {
        UUID schoolId = TenantContext.getCurrentTenant();
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        if (!invoice.getSchool().getId().equals(schoolId)) {
            throw new SecurityException("Unauthorized access to tenant invoice.");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }

        BigDecimal updatedPaidAmount = invoice.getPaidAmount().add(amount);
        invoice.setPaidAmount(updatedPaidAmount);
        invoice.recalculateBalance();

        // Transaction log can be saved here to database
        invoiceRepository.save(invoice);
        return true;
    }

    private InvoiceResponse mapToResponse(Invoice invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .title(invoice.getTitle())
                .totalAmount(invoice.getTotalAmount())
                .discountAmount(invoice.getDiscountAmount())
                .lateFeeAmount(invoice.getLateFeeAmount())
                .paidAmount(invoice.getPaidAmount())
                .balanceAmount(invoice.getBalanceAmount())
                .dueDate(invoice.getDueDate())
                .status(invoice.getStatus().name())
                .build();
    }
}
