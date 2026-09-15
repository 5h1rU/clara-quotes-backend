package com.felipejaner.quotes.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Clock;
import java.time.Duration;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
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
}
