package com.schoolerp.school_erp.strategy;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component("daily")
public class DailyLateFeeStrategy implements LateFeeStrategy {

    @Override
    public BigDecimal calculateLateFee(BigDecimal baseAmount, LocalDate dueDate, BigDecimal ratePerInterval) {
        LocalDate today = LocalDate.now();
        if (today.isBefore(dueDate) || today.isEqual(dueDate)) {
            return BigDecimal.ZERO;
        }

        long daysOverdue = ChronoUnit.DAYS.between(dueDate, today);
        return ratePerInterval.multiply(BigDecimal.valueOf(daysOverdue));
    }
}
