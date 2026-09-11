package com.felipejaner.quotes.domain;

import java.util.Set;

public record HealthDetails(
    boolean hasPreexistingConditions,
    Set<Condition> conditions,
    boolean takesPrescriptionMedication,
    boolean usesTobacco,
    boolean needsSpouseCoverage) {
  public HealthDetails {
    conditions = Set.copyOf(conditions);
  }
}
