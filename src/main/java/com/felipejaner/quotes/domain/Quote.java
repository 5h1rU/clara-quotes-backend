package com.felipejaner.quotes.domain;

import com.felipejaner.quotes.error.InvalidQuoteStateException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "quotes")
public class Quote {
  @Id private UUID id;
  @Version private long version;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String email;

  @Column(nullable = false)
  private int age;

  @Column(nullable = false)
  private String zipCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private QuoteStatus status;

  @Enumerated(EnumType.STRING)
  private CoverageType coverageType;

  private Boolean hasPreexistingConditions;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "quote_conditions", joinColumns = @JoinColumn(name = "quote_id"))
  @Column(name = "condition")
  @Enumerated(EnumType.STRING)
  private Set<Condition> conditions = new HashSet<>();

  private Boolean takesPrescriptionMedication;
  private Boolean usesTobacco;
  private Boolean needsSpouseCoverage;

  @Column(precision = 10, scale = 2)
  private BigDecimal estimatedMonthlyPremium;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  private Instant submittedAt;

  protected Quote() {}

  public Quote(String name, String email, int age, String zipCode, Instant now) {
    this.id = UUID.randomUUID();
    this.name = name.strip();
    this.email = email.strip();
    this.age = age;
    this.zipCode = zipCode;
    this.status = QuoteStatus.DRAFT;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public void selectCoverage(
      CoverageType type, HealthDetails health, BigDecimal premium, Instant now) {
    requireEditable();
    this.coverageType = type;
    this.hasPreexistingConditions = health == null ? null : health.hasPreexistingConditions();
    this.conditions = new HashSet<>(health == null ? Set.of() : health.conditions());
    this.takesPrescriptionMedication = health == null ? null : health.takesPrescriptionMedication();
    this.usesTobacco = health == null ? null : health.usesTobacco();
    this.needsSpouseCoverage = health == null ? null : health.needsSpouseCoverage();
    this.estimatedMonthlyPremium = premium;
    this.updatedAt = now;
  }

  public void requireEditable() {
    if (status != QuoteStatus.DRAFT && status != QuoteStatus.SUBMISSION_FAILED)
      throw new InvalidQuoteStateException(
          "A " + status + " quote cannot be changed or submitted.");
  }

  public void requireSubmittable() {
    requireEditable();
    if (coverageType == null
        || estimatedMonthlyPremium == null
        || (age > 65
            && (hasPreexistingConditions == null
                || takesPrescriptionMedication == null
                || usesTobacco == null
                || needsSpouseCoverage == null)))
      throw new InvalidQuoteStateException("Complete coverage selection before submitting.");
  }

  public void markSubmitted(Instant now) {
    requireSubmittable();
    status = QuoteStatus.SUBMITTED;
    submittedAt = now;
    updatedAt = now;
  }

  public void markSubmissionFailed(Instant now) {
    requireSubmittable();
    status = QuoteStatus.SUBMISSION_FAILED;
    updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public int getAge() {
    return age;
  }

  public String getZipCode() {
    return zipCode;
  }

  public QuoteStatus getStatus() {
    return status;
  }

  public CoverageType getCoverageType() {
    return coverageType;
  }

  public Boolean getHasPreexistingConditions() {
    return hasPreexistingConditions;
  }

  public Set<Condition> getConditions() {
    return Set.copyOf(conditions);
  }

  public Boolean getTakesPrescriptionMedication() {
    return takesPrescriptionMedication;
  }

  public Boolean getUsesTobacco() {
    return usesTobacco;
  }

  public Boolean getNeedsSpouseCoverage() {
    return needsSpouseCoverage;
  }

  public BigDecimal getEstimatedMonthlyPremium() {
    return estimatedMonthlyPremium;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }
}
