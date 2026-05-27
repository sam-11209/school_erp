package com.schoolerp.school_erp.controller;

import com.schoolerp.school_erp.service.BillingService;
import com.schoolerp.school_erp.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Controller to handle administrative tasks such as recording billing payments,
 * managing student status, and assigning sections.
 * 
 * Constraints:
 * - Secured under "/api/admin/**", requiring a valid authentication token for all endpoints.
 * - Requires the X-Tenant-ID header to determine the school tenant scope.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private BillingService billingService;

    @Autowired
    private StudentService studentService;

    /**
     * Records a payment (partial or full) against a student's invoice.
     * 
     * Constraints:
     * - Requires standard JWT in Authorization header (Bearer token).
     * - Requires X-Tenant-ID header to resolve the tenant database context.
     * 
     * @param invoiceId the ID of the invoice being paid
     * @param amount the payment amount
     * @param method the payment method (e.g., Cash, Card, Online)
     * @param reference reference code or transaction ID of the payment
     * @return response indicating success or failure of the payment entry
     */
    @PostMapping("/billing/payment")
    public ResponseEntity<String> recordPayment(
            @RequestParam UUID invoiceId,
            @RequestParam BigDecimal amount,
            @RequestParam String method,
            @RequestParam String reference) {
        
        boolean success = billingService.recordPartialPayment(invoiceId, amount, method, reference);
        if (success) {
            return ResponseEntity.ok("Payment recorded successfully");
        }
        return ResponseEntity.badRequest().body("Failed to record payment");
    }

    /**
     * Updates the status (active/inactive) of a student profile.
     * 
     * Constraints:
     * - Requires standard JWT in Authorization header (Bearer token).
     * - Requires X-Tenant-ID header to resolve the tenant context.
     * 
     * @param id the student profile ID to update
     * @param isActive target status for the student profile
     * @return response indicating success or failure of the status update
     */
    @PutMapping("/students/{id}/status")
    public ResponseEntity<String> updateStudentStatus(
            @PathVariable UUID id,
            @RequestParam boolean isActive) {
        
        boolean success = studentService.updateProfileStatus(id, isActive);
        if (success) {
            return ResponseEntity.ok("Student status updated successfully");
        }
        return ResponseEntity.badRequest().body("Failed to update student status");
    }

    /**
     * Assigns a student to a specific class section.
     * 
     * Constraints:
     * - Requires standard JWT in Authorization header (Bearer token).
     * - Requires X-Tenant-ID header to resolve the tenant context.
     * 
     * @param id the student profile ID to assign
     * @param sectionId the section ID to assign the student to
     * @return response indicating success or failure of the section assignment
     */
    @PutMapping("/students/{id}/assign-section")
    public ResponseEntity<String> assignSection(
            @PathVariable UUID id,
            @RequestParam UUID sectionId) {
        
        boolean success = studentService.assignSection(id, sectionId);
        if (success) {
            return ResponseEntity.ok("Student section assigned successfully");
        }
        return ResponseEntity.badRequest().body("Failed to assign student section");
    }
}
