package com.felipejaner.quotes.application;


import com.felipejaner.quotes.api.*;
import com.felipejaner.quotes.domain.Quote;
import com.felipejaner.quotes.error.QuoteNotFoundException;
import com.felipejaner.quotes.persistence.QuoteRepository;
import com.felipejaner.quotes.pricing.PremiumCalculator;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
@Service
public class QuoteService {
    private final QuoteRepository repository;
    private final CoverageValidator validator;
    private final PremiumCalculator calculator;
    private final Clock clock;
    private final ApplicationEventPublisher events;
    public QuoteService(QuoteRepository repository, CoverageValidator validator, PremiumCalculator calculator,
                        Clock clock, ApplicationEventPublisher events) {
        this.repository = repository; this.validator = validator; this.calculator = calculator;
        this.clock = clock; this.events = events;
    }
    @Transactional
    public QuoteResponse create(CreateQuoteRequest request) {
        return QuoteResponse.from(repository.save(new Quote(request.name(), request.email(), request.age(), request.zipCode(), clock.instant())));
    }
    @Cacheable(value = "quotes", key = "#id")
    @Transactional(readOnly = true)
    public QuoteResponse get(UUID id) {
        return QuoteResponse.from(repository.findById(id).orElseThrow(() -> new QuoteNotFoundException("Quote was not found.")));
    }
    @Transactional(readOnly = true)
    public List<QuoteResponse> list() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream().map(QuoteResponse::from).toList();
    }
    @Transactional
    public QuoteResponse updateCoverage(UUID id, CoverageRequest request) {
        Quote quote = repository.findForUpdate(id).orElseThrow(() -> new QuoteNotFoundException("Quote was not found."));
        quote.requireEditable();
        var health = validator.validate(quote.getAge(), request);
        quote.selectCoverage(request.coverageType(), health, calculator.calculate(request.coverageType(), quote.getAge(), health), clock.instant());
        events.publishEvent(new QuoteChanged(id));
        return QuoteResponse.from(quote);
    }
}
