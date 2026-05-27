package com.schoolerp.school_erp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "invoice_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "fee_category_id")
    private UUID feeCategoryId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "manual_discount_amount", nullable = false)
    @Builder.Default
    private BigDecimal manualDiscountAmount = BigDecimal.ZERO;

    private String notes;
}
