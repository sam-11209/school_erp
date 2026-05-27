package com.schoolerp.school_erp.controller;

import com.schoolerp.school_erp.dto.InvoiceResponse;
import com.schoolerp.school_erp.dto.StudentProfileResponse;
import com.schoolerp.school_erp.service.BillingService;
import com.schoolerp.school_erp.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller to manage student-specific requests such as profiles, invoices,
 * and fee calculations.
 * 
 * Constraints:
 * - Secured under "/api/student/**", requiring a valid authentication token.
 * - Requires X-Tenant-ID header to resolve database tenant context.
 */
@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private BillingService billingService;

    /**
     * Retrieves the student profile details.
     * 
     * Constraints:
     * - Requires standard JWT in Authorization header.
     * - Requires X-Tenant-ID header.
     * - Takes student profile ID as a path variable.
     * 
     * @param id the student profile ID
     * @return response indicating student personal and academic details
     */
    @GetMapping("/profile/{id}")
    public ResponseEntity<StudentProfileResponse> getProfile(@PathVariable UUID id) {
        StudentProfileResponse response = studentService.getProfile(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all billing invoices associated with a specific student.
     * 
     * Constraints:
     * - Requires standard JWT in Authorization header.
     * - Requires X-Tenant-ID header.
     * - Expects the student's user/profile ID as a query parameter.
     * 
     * @param studentId the student ID to retrieve invoices for
     * @return list of invoices for the specified student
     */
    @GetMapping("/invoices")
    public ResponseEntity<List<InvoiceResponse>> getInvoices(@RequestParam UUID studentId) {
        List<InvoiceResponse> response = billingService.getStudentInvoices(studentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Re-calculates fees and checks for any applicable late fees on a specific student invoice.
     * 
     * Constraints:
     * - Requires standard JWT in Authorization header.
     * - Requires X-Tenant-ID header.
     * - Takes target invoice ID as a path variable.
     * 
     * @param id the invoice ID
     * @return updated invoice details with late fees calculations applied
     */
    @PostMapping("/invoices/{id}/calculate")
    public ResponseEntity<InvoiceResponse> calculateFees(@PathVariable UUID id) {
        InvoiceResponse response = billingService.calculateFeesAndLateFees(id);
        return ResponseEntity.ok(response);
    }
}
