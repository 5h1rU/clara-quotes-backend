package com.felipejaner.quotes.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxPublisher {
  private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
  private final OutboxRepository repository;
  private final KafkaTemplate<String, String> kafka;
  private final ObjectMapper mapper;
  private final Clock clock;
  private final String topic;

  public OutboxPublisher(
      OutboxRepository repository,
      KafkaTemplate<String, String> kafka,
      ObjectMapper mapper,
      Clock clock,
      @Value("${quotes.kafka.topic}") String topic) {
    this.repository = repository;
    this.kafka = kafka;
    this.mapper = mapper;
    this.clock = clock;
    this.topic = topic;
  }

  @Transactional
  public void publishPending() {
    for (OutboxEvent event : repository.lockPendingBatch()) {
      try {
        kafka
            .send(
                topic,
                event.getQuoteId().toString(),
                mapper.writeValueAsString(QuoteSubmittedEvent.from(event)))
            .get(5, TimeUnit.SECONDS);
        event.markPublished(clock.instant());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      } catch (ExecutionException
          | TimeoutException
          | JsonProcessingException
          | org.springframework.kafka.KafkaException
          | org.apache.kafka.common.KafkaException e) {
        // Keep pending. A crash after acknowledgement can redeliver the same eventId.
        log.warn(
            "Outbox delivery deferred for event {} ({})",
            event.getId(),
            e.getClass().getSimpleName());
        return;
      }
    }
  }
}
