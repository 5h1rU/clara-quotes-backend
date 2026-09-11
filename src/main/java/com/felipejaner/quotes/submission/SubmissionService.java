package com.felipejaner.quotes.submission;

import com.felipejaner.quotes.api.QuoteResponse;
import com.felipejaner.quotes.application.QuoteChanged;
import com.felipejaner.quotes.domain.*;
import com.felipejaner.quotes.error.*;
import com.felipejaner.quotes.messaging.*;
import com.felipejaner.quotes.persistence.QuoteRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmissionService {
  private final QuoteRepository quotes;
  private final InsurerGateway insurer;
  private final OutboxRepository outbox;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public SubmissionService(
      QuoteRepository quotes,
      InsurerGateway insurer,
      OutboxRepository outbox,
      ApplicationEventPublisher events,
      Clock clock) {
    this.quotes = quotes;
    this.insurer = insurer;
    this.outbox = outbox;
    this.events = events;
    this.clock = clock;
  }

  // Expected network failure must COMMIT SUBMISSION_FAILED, not roll it back.
  @Transactional(noRollbackFor = InsurerUnavailableException.class)
  public QuoteResponse submit(UUID id) {
    Quote quote =
        quotes
            .findForUpdate(id)
            .orElseThrow(() -> new QuoteNotFoundException("Quote was not found."));
    if (quote.getStatus() == QuoteStatus.SUBMITTED) return QuoteResponse.from(quote);
    quote.requireSubmittable();
    try {
      insurer.submit(id);
    } catch (InsurerUnavailableException e) {
      quote.markSubmissionFailed(clock.instant());
      events.publishEvent(new QuoteChanged(id));
      throw e;
    }
    quote.markSubmitted(clock.instant());
    outbox.save(new OutboxEvent(id, quote.getEstimatedMonthlyPremium(), clock.instant()));
    events.publishEvent(new QuoteChanged(id));
    return QuoteResponse.from(quote);
  }
}
