package com.felipejaner.quotes.application;

import com.felipejaner.quotes.api.QuoteResponse;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class QuoteCache {
  private record Key(UUID id, long generation) {}

  private final Cache cache;
  private final AtomicLong generation = new AtomicLong();

  public QuoteCache(CacheManager manager) {
    cache = Objects.requireNonNull(manager.getCache("quotes"));
  }

  public QuoteResponse get(UUID id, Supplier<QuoteResponse> loader) {
    var key = new Key(id, generation.get());
    // Use separate get/put calls so cache loader wrapping cannot hide domain exceptions.
    var saved = cache.get(key, QuoteResponse.class);
    if (saved != null) return saved;
    var loaded = loader.get();
    cache.put(key, loaded);
    return loaded;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onChange(QuoteChanged event) {
    // An older in-flight read may still put its result after clear. Its generation is
    // no longer used, so later reads cannot resurrect that stale response.
    generation.incrementAndGet();
    cache.clear();
  }
}
