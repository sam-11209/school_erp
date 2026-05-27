package com.schoolerp.school_erp.strategy;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface LateFeeStrategy {
    BigDecimal calculateLateFee(BigDecimal baseAmount, LocalDate dueDate, BigDecimal ratePerInterval);
}
