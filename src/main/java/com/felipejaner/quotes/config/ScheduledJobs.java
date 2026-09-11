package com.felipejaner.quotes.config;

import com.felipejaner.quotes.application.DraftExpirationService;
import com.felipejaner.quotes.messaging.OutboxPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;

@Configuration
@EnableScheduling
@ConditionalOnProperty(
    name = "quotes.scheduling.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class ScheduledJobs {
  private final DraftExpirationService expiration;
  private final OutboxPublisher outbox;

  public ScheduledJobs(DraftExpirationService expiration, OutboxPublisher outbox) {
    this.expiration = expiration;
    this.outbox = outbox;
  }

  @Scheduled(fixedDelayString = "${quotes.expiration.interval}")
  public void expireDrafts() {
    expiration.expire();
  }

  @Scheduled(fixedDelayString = "${quotes.outbox.interval}")
  public void publishEvents() {
    outbox.publishPending();
  }
}
