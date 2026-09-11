package com.felipejaner.quotes.application;

import java.util.UUID;

/** Published inside a transaction; cache invalidation runs only after commit. */
public record QuoteChanged(UUID id) {}
