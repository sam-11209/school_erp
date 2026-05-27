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

@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private BillingService billingService;

    @GetMapping("/profile/{id}")
    public ResponseEntity<StudentProfileResponse> getProfile(@PathVariable UUID id) {
        StudentProfileResponse response = studentService.getProfile(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/invoices")
    public ResponseEntity<List<InvoiceResponse>> getInvoices(@RequestParam UUID studentId) {
        List<InvoiceResponse> response = billingService.getStudentInvoices(studentId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/invoices/{id}/calculate")
    public ResponseEntity<InvoiceResponse> calculateFees(@PathVariable UUID id) {
        InvoiceResponse response = billingService.calculateFeesAndLateFees(id);
        return ResponseEntity.ok(response);
    }
}
