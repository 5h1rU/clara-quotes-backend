package com.felipejaner.quotes.messaging;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
  @Id private UUID id;

  @Column(nullable = false, unique = true)
  private UUID quoteId;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal premium;

  @Column(nullable = false)
  private Instant occurredAt;

  private Instant publishedAt;

  protected OutboxEvent() {}

  public OutboxEvent(UUID quoteId, BigDecimal premium, Instant now) {
    this.id = UUID.randomUUID();
    this.quoteId = quoteId;
    this.premium = premium;
    this.occurredAt = now;
  }

  public UUID getId() {
    return id;
  }

  public UUID getQuoteId() {
    return quoteId;
  }

  public BigDecimal getPremium() {
    return premium;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public void markPublished(Instant now) {
    this.publishedAt = now;
  }
}
