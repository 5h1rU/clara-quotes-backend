package com.felipejaner.quotes.messaging;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
public record QuoteSubmittedEvent(UUID eventId, String type, int schemaVersion, UUID quoteId,
                                  BigDecimal estimatedMonthlyPremium, Instant occurredAt) {
    public static QuoteSubmittedEvent from(OutboxEvent event) {
        return new QuoteSubmittedEvent(event.getId(), "QuoteSubmitted", 1, event.getQuoteId(), event.getPremium(), event.getOccurredAt());
    }
}
