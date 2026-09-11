package com.felipejaner.quotes;

import com.felipejaner.quotes.domain.*;
import com.felipejaner.quotes.pricing.PremiumCalculator;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
class PremiumCalculatorTest {
    private final PremiumCalculator calculator = new PremiumCalculator();
    @Test void matchesAssignmentExampleExactly() {
        assertThat(calculator.calculate(CoverageType.STANDARD, 70, new HealthDetails(true, Set.of(Condition.DIABETES), true, true, true)))
            .isEqualByComparingTo("327.60");
    }
    @Test void age65DoesNotPaySeniorMultiplier() {
        assertThat(calculator.calculate(CoverageType.BASIC, 65, null)).isEqualByComparingTo("50.00");
        assertThat(calculator.calculate(CoverageType.BASIC, 66, new HealthDetails(false, Set.of(), false, false, false))).isEqualByComparingTo("75.00");
    }
    @Test void multipleConditionsApplyOnlyOneFactorAndMedicationDoesNotAffectPrice() {
        var health = new HealthDetails(true, Set.of(Condition.DIABETES, Condition.OTHER), true, false, false);
        assertThat(calculator.calculate(CoverageType.PREMIUM, 70, health)).isEqualByComparingTo("390.00");
        assertThat(calculator.calculate(CoverageType.STANDARD, 30, null)).isEqualByComparingTo("100.00");
    }
}
