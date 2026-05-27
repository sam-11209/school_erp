package com.schoolerp.school_erp.service;

import com.schoolerp.school_erp.dto.InvoiceResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface BillingService {
    List<InvoiceResponse> getStudentInvoices(UUID studentId);
    InvoiceResponse calculateFeesAndLateFees(UUID invoiceId);
    boolean recordPartialPayment(UUID invoiceId, BigDecimal amount, String method, String reference);
}
