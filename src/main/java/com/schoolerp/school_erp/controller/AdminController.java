package com.schoolerp.school_erp.controller;

import com.schoolerp.school_erp.service.BillingService;
import com.schoolerp.school_erp.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private BillingService billingService;

    @Autowired
    private StudentService studentService;

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
