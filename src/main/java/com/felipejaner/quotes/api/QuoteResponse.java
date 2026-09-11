package com.felipejaner.quotes.api;

import com.felipejaner.quotes.domain.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record QuoteResponse(
    UUID id,
    String name,
    String email,
    int age,
    String zipCode,
    QuoteStatus status,
    CoverageType coverageType,
    Boolean hasPreexistingConditions,
    Set<Condition> conditions,
    Boolean takesPrescriptionMedication,
    Boolean usesTobacco,
    Boolean needsSpouseCoverage,
    BigDecimal estimatedMonthlyPremium,
    Instant createdAt,
    Instant updatedAt,
    Instant submittedAt) {
  public static QuoteResponse from(Quote q) {
    return new QuoteResponse(
        q.getId(),
        q.getName(),
        q.getEmail(),
        q.getAge(),
        q.getZipCode(),
        q.getStatus(),
        q.getCoverageType(),
        q.getHasPreexistingConditions(),
        q.getConditions(),
        q.getTakesPrescriptionMedication(),
        q.getUsesTobacco(),
        q.getNeedsSpouseCoverage(),
        q.getEstimatedMonthlyPremium(),
        q.getCreatedAt(),
        q.getUpdatedAt(),
        q.getSubmittedAt());
  }
}
