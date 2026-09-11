package com.felipejaner.quotes.config;

import com.felipejaner.quotes.application.QuoteChanged;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Clock;
import java.time.Duration;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.transaction.event.*;

@Configuration
@EnableCaching
public class InfrastructureConfig {
  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  CacheManager cacheManager() {
    var manager = new CaffeineCacheManager("quotes");
    manager.setCaffeine(
        Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(Duration.ofSeconds(30)));
    return manager;
  }

  @Bean
  NewTopic quoteTopic(@Value("${quotes.kafka.topic}") String topic) {
    return TopicBuilder.name(topic).partitions(1).replicas(1).build();
  }

  @Bean
  CacheInvalidation cacheInvalidation(CacheManager manager) {
    return new CacheInvalidation(manager);
  }

  public static class CacheInvalidation {
    private final CacheManager manager;

    CacheInvalidation(CacheManager manager) {
      this.manager = manager;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChange(QuoteChanged event) {
      var cache = manager.getCache("quotes");
      if (cache != null) {
        if (event.id() == null) cache.clear();
        else cache.evict(event.id());
      }
    }
  }
}
