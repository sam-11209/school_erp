package com.schoolerp.school_erp.service.impl;

import com.schoolerp.school_erp.dto.PaymentOrderRequest;
import com.schoolerp.school_erp.dto.PaymentOrderResponse;
import com.schoolerp.school_erp.entity.*;
import com.schoolerp.school_erp.filter.TenantContext;
import com.schoolerp.school_erp.repository.*;
import com.schoolerp.school_erp.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentGatewayRepository paymentGatewayRepository;

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Override
    @Transactional
    public PaymentOrderResponse createPaymentOrder(PaymentOrderRequest request) {
        UUID schoolId = TenantContext.getCurrentTenant();
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new IllegalArgumentException("School not found"));

        Invoice invoice = invoiceRepository.findByIdAndSchoolIdAndDeletedAtIsNull(request.getInvoiceId(), schoolId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        // Verify gateway details configured
        PaymentGateway gateway = paymentGatewayRepository
                .findBySchoolIdAndProviderNameAndDeletedAtIsNull(schoolId, request.getProviderName().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Payment gateway not configured for provider: " + request.getProviderName()));

        if (!gateway.getIsActive()) {
            throw new IllegalStateException("Payment provider gateway is currently inactive");
        }

        String prefix = request.getProviderName().toLowerCase().startsWith("stripe") ? "order_stripe_" : "order_rzp_";
        String gatewayOrderId = prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String checkoutUrl = request.getProviderName().equalsIgnoreCase("stripe") 
                ? "https://checkout.stripe.com/pay/" + gatewayOrderId 
                : "https://api.razorpay.com/v1/checkout/" + gatewayOrderId;

        PaymentOrder paymentOrder = PaymentOrder.builder()
                .school(school)
                .invoice(invoice)
                .amount(request.getAmount())
                .gatewayOrderId(gatewayOrderId)
                .status("PENDING")
                .build();

        paymentOrder = paymentOrderRepository.save(paymentOrder);

        return PaymentOrderResponse.builder()
                .orderId(paymentOrder.getId())
                .invoiceId(invoice.getId())
                .amount(paymentOrder.getAmount())
                .gatewayOrderId(gatewayOrderId)
                .gatewaySessionUrl(checkoutUrl)
                .providerName(gateway.getProviderName())
                .status(paymentOrder.getStatus())
                .build();
    }

    @Override
    @Transactional
    public PaymentOrder verifyPayment(String gatewayOrderId, String gatewayPaymentId, String status) {
        UUID schoolId = TenantContext.getCurrentTenant();
        PaymentOrder order = paymentOrderRepository
                .findByGatewayOrderIdAndSchoolIdAndDeletedAtIsNull(gatewayOrderId, schoolId)
                .orElseThrow(() -> new IllegalArgumentException("Payment order not found: " + gatewayOrderId));

        order.setStatus(status.toUpperCase());
        order.setGatewayPaymentId(gatewayPaymentId);

        if (status.equalsIgnoreCase("SUCCESS")) {
            Invoice invoice = order.getInvoice();
            BigDecimal amountPaid = order.getAmount();
            BigDecimal currentPaid = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal newPaid = currentPaid.add(amountPaid);
            invoice.setPaidAmount(newPaid);
            invoice.recalculateBalance();

            invoiceRepository.save(invoice);
        }

        return paymentOrderRepository.save(order);
    }
}
