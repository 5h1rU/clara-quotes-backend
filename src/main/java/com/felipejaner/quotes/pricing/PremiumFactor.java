package com.felipejaner.quotes.pricing;


import com.felipejaner.quotes.domain.HealthDetails;
import java.math.BigDecimal;
@FunctionalInterface
public interface PremiumFactor {
    BigDecimal multiplier(int age, HealthDetails health);
}
