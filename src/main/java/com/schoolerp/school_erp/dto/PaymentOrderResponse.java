package com.schoolerp.school_erp.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class PaymentOrderResponse {
    private UUID orderId;
    private UUID invoiceId;
    private BigDecimal amount;
    private String gatewayOrderId;
    private String gatewaySessionUrl; // relevant for Stripe checkout
    private String providerName;
    private String status;
}
