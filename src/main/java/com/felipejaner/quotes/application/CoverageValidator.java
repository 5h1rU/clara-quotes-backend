package com.felipejaner.quotes.application;

import com.felipejaner.quotes.api.CoverageRequest;
import com.felipejaner.quotes.domain.HealthDetails;
import com.felipejaner.quotes.error.QuoteValidationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CoverageValidator {
  public HealthDetails validate(int age, CoverageRequest r) {
    Map<String, String> errors = new LinkedHashMap<>();
    if (age <= 65) {
      if (r.hasPreexistingConditions() != null
          || r.conditions() != null
          || r.takesPrescriptionMedication() != null
          || r.usesTobacco() != null
          || r.needsSpouseCoverage() != null)
        errors.put("health", "Health fields are only allowed when age is greater than 65.");
      if (!errors.isEmpty()) throw new QuoteValidationException(errors);
      return null;
    }
    if (r.hasPreexistingConditions() == null)
      errors.put("hasPreexistingConditions", "Choose Yes or No.");
    if (r.takesPrescriptionMedication() == null)
      errors.put("takesPrescriptionMedication", "Choose Yes or No.");
    if (r.usesTobacco() == null) errors.put("usesTobacco", "Choose Yes or No.");
    if (r.needsSpouseCoverage() == null) errors.put("needsSpouseCoverage", "Choose Yes or No.");
    var conditions =
        r.conditions() == null ? Set.<com.felipejaner.quotes.domain.Condition>of() : r.conditions();
    if (Boolean.TRUE.equals(r.hasPreexistingConditions()) && conditions.isEmpty())
      errors.put("conditions", "Select at least one condition.");
    if (Boolean.FALSE.equals(r.hasPreexistingConditions()) && !conditions.isEmpty())
      errors.put("conditions", "Conditions must be empty when the answer is No.");
    if (!errors.isEmpty()) throw new QuoteValidationException(errors);
    return new HealthDetails(
        r.hasPreexistingConditions(),
        conditions,
        r.takesPrescriptionMedication(),
        r.usesTobacco(),
        r.needsSpouseCoverage());
  }
}
