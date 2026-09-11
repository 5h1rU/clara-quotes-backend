package com.felipejaner.quotes;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.felipejaner.quotes.domain.*;
import com.felipejaner.quotes.error.*;
import com.felipejaner.quotes.messaging.*;
import com.felipejaner.quotes.persistence.QuoteRepository;
import com.felipejaner.quotes.submission.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class SubmissionServiceTest {
  final QuoteRepository quotes = mock(QuoteRepository.class);
  final InsurerGateway insurer = mock(InsurerGateway.class);
  final OutboxRepository outbox = mock(OutboxRepository.class);
  final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
  final Clock clock = Clock.fixed(Instant.parse("2026-09-11T12:00:00Z"), ZoneOffset.UTC);
  final SubmissionService service = new SubmissionService(quotes, insurer, outbox, events, clock);
  Quote quote;

  @BeforeEach
  void setup() {
    quote = new Quote("Test", "test@example.com", 30, "90210", clock.instant());
    when(quotes.findForUpdate(quote.getId())).thenReturn(Optional.of(quote));
  }

  void cover() {
    quote.selectCoverage(CoverageType.BASIC, null, new BigDecimal("50.00"), clock.instant());
  }

  @Test
  void submitsAndRepeatedSubmissionDoesNotRepeatSideEffects() {
    cover();
    assertThat(service.submit(quote.getId()).status()).isEqualTo(QuoteStatus.SUBMITTED);
    service.submit(quote.getId());
    verify(insurer, times(1)).submit(quote.getId());
    verify(outbox, times(1)).save(any(OutboxEvent.class));
    assertThatThrownBy(
            () ->
                quote.selectCoverage(
                    CoverageType.PREMIUM, null, new BigDecimal("200"), clock.instant()))
        .isInstanceOf(InvalidQuoteStateException.class);
  }

  @Test
  void failedSubmissionCanRetryWithoutLosingCoverage() {
    cover();
    doThrow(new InsurerUnavailableException("timeout"))
        .doNothing()
        .when(insurer)
        .submit(quote.getId());
    assertThatThrownBy(() -> service.submit(quote.getId()))
        .isInstanceOf(InsurerUnavailableException.class);
    assertThat(quote.getStatus()).isEqualTo(QuoteStatus.SUBMISSION_FAILED);
    verifyNoInteractions(outbox);
    assertThat(service.submit(quote.getId()).status()).isEqualTo(QuoteStatus.SUBMITTED);
    verify(outbox).save(any(OutboxEvent.class));
  }

  @Test
  void incompleteQuoteNeverCallsTheInsurer() {
    assertThatThrownBy(() -> service.submit(quote.getId()))
        .isInstanceOf(InvalidQuoteStateException.class);
    verifyNoInteractions(insurer, outbox);
  }
}
