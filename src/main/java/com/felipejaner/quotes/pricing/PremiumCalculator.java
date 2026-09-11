package com.felipejaner.quotes.pricing;


import com.felipejaner.quotes.domain.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PremiumCalculator {
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final Map<CoverageType, BigDecimal> BASE = Map.of(
        CoverageType.BASIC, new BigDecimal("50"), CoverageType.STANDARD, new BigDecimal("100"),
        CoverageType.PREMIUM, new BigDecimal("200"));
    private final List<PremiumFactor> factors;
    public PremiumCalculator() {
        this(List.of(
            (age, h) -> age > 65 ? new BigDecimal("1.5") : ONE,
            (age, h) -> h != null && !h.conditions().isEmpty() ? new BigDecimal("1.3") : ONE,
            (age, h) -> h != null && h.usesTobacco() ? new BigDecimal("1.2") : ONE,
            (age, h) -> h != null && h.needsSpouseCoverage() ? new BigDecimal("1.4") : ONE));
    }
    public PremiumCalculator(List<PremiumFactor> factors) { this.factors = List.copyOf(factors); }
    public BigDecimal calculate(CoverageType coverage, int age, HealthDetails health) {
        BigDecimal amount = BASE.get(coverage);
        for (PremiumFactor factor : factors) amount = amount.multiply(factor.multiplier(age, health));
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
