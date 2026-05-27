package com.schoolerp.school_erp.service;

import com.schoolerp.school_erp.dto.PaymentOrderRequest;
import com.schoolerp.school_erp.dto.PaymentOrderResponse;
import com.schoolerp.school_erp.entity.PaymentOrder;

import java.util.UUID;

public interface PaymentService {
    PaymentOrderResponse createPaymentOrder(PaymentOrderRequest request);
    PaymentOrder verifyPayment(String gatewayOrderId, String gatewayPaymentId, String status);
}
