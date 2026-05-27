package com.schoolerp.school_erp.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Component
public class LateFeeCalculator {

    private final Map<String, LateFeeStrategy> strategies;

    @Autowired
    public LateFeeCalculator(Map<String, LateFeeStrategy> strategies) {
        this.strategies = strategies;
    }

    public BigDecimal calculate(String strategyType, BigDecimal baseAmount, LocalDate dueDate, BigDecimal ratePerInterval) {
        LateFeeStrategy strategy = strategies.get(strategyType.toLowerCase());
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown late fee strategy: " + strategyType);
        }
        return strategy.calculateLateFee(baseAmount, dueDate, ratePerInterval);
    }
}
