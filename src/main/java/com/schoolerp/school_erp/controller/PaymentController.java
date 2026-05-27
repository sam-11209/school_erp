package com.schoolerp.school_erp.controller;

import com.schoolerp.school_erp.dto.PaymentOrderRequest;
import com.schoolerp.school_erp.dto.PaymentOrderResponse;
import com.schoolerp.school_erp.entity.PaymentOrder;
import com.schoolerp.school_erp.entity.User;
import com.schoolerp.school_erp.enums.RoleType;
import com.schoolerp.school_erp.repository.UserRepository;
import com.schoolerp.school_erp.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/checkout")
    public ResponseEntity<?> createCheckoutSession(
            @Valid @RequestBody PaymentOrderRequest request,
            @RequestHeader("X-User-ID") UUID customerUserId) {

        User user = userRepository.findById(customerUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isAuthorized = user.getRoles().stream().anyMatch(role -> 
            role.getName().equalsIgnoreCase("SUPER_ADMIN") || 
            role.getName().equalsIgnoreCase("ADMIN") || 
            role.getName().equalsIgnoreCase("STUDENT") || 
            role.getName().equalsIgnoreCase("PARENT")
        );

        if (!isAuthorized) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Unauthorized: Only Students, Parents, or Admins can make payments.");
        }

        PaymentOrderResponse response = paymentService.createPaymentOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<PaymentOrder> verifyPayment(
            @RequestParam String gatewayOrderId,
            @RequestParam String gatewayPaymentId,
            @RequestParam String status) {

        PaymentOrder response = paymentService.verifyPayment(gatewayOrderId, gatewayPaymentId, status);
        return ResponseEntity.ok(response);
    }
}
