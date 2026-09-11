package com.felipejaner.quotes.application;


import com.felipejaner.quotes.persistence.QuoteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Duration;
@Service
public class DraftExpirationService {
    private final QuoteRepository repository;
    private final Clock clock;
    private final Duration ttl;
    private final ApplicationEventPublisher events;
    public DraftExpirationService(QuoteRepository repository, Clock clock,
            @Value("${quotes.expiration.ttl}") Duration ttl, ApplicationEventPublisher events) {
        this.repository = repository; this.clock = clock; this.ttl = ttl; this.events = events;
    }
    @Transactional
    public int expire() {
        var now = clock.instant();
        int count = repository.expireDrafts(now.minus(ttl), now);
        if (count > 0) events.publishEvent(new QuoteChanged(null));
        return count;
    }
}
