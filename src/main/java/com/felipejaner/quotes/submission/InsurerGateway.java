package com.felipejaner.quotes.submission;

import java.util.UUID;

/** A port: business logic needs acceptance, not an HTTP client implementation. */
public interface InsurerGateway {
  void submit(UUID quoteId);
}
