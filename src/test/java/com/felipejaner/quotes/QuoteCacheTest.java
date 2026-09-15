package com.felipejaner.quotes;

import static org.assertj.core.api.Assertions.*;

import com.felipejaner.quotes.api.QuoteResponse;
import com.felipejaner.quotes.application.QuoteCache;
import com.felipejaner.quotes.application.QuoteChanged;
import com.felipejaner.quotes.domain.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.cache.caffeine.CaffeineCacheManager;

class QuoteCacheTest {
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void lateReadCannotRefillCurrentGenerationAfterInvalidation(boolean bulk) throws Exception {
    var cache = new QuoteCache(new CaffeineCacheManager("quotes"));
    var quote = new Quote("Test", "test@example.com", 30, "90210", Instant.now());
    var old = QuoteResponse.from(quote);
    quote.selectCoverage(CoverageType.BASIC, null, new BigDecimal("50"), Instant.now());
    var latest = QuoteResponse.from(quote);
    var loaded = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var executor = Executors.newSingleThreadExecutor();
    try {
      var lateRead =
          executor.submit(
              () ->
                  cache.get(
                      quote.getId(),
                      () -> {
                        loaded.countDown();
                        try {
                          if (!release.await(5, TimeUnit.SECONDS))
                            throw new AssertionError("Read was not released");
                        } catch (InterruptedException e) {
                          Thread.currentThread().interrupt();
                          throw new AssertionError(e);
                        }
                        return old;
                      }));
      assertThat(loaded.await(5, TimeUnit.SECONDS)).isTrue();
      cache.onChange(new QuoteChanged(bulk ? null : quote.getId()));
      assertThat(cache.get(quote.getId(), () -> latest)).isEqualTo(latest);
      release.countDown();
      assertThat(lateRead.get(5, TimeUnit.SECONDS)).isEqualTo(old);
      assertThat(
              cache.get(
                  quote.getId(),
                  () -> {
                    throw new AssertionError("Expected cache hit");
                  }))
          .isEqualTo(latest);
    } finally {
      release.countDown();
      executor.shutdownNow();
    }
  }
}
