package com.felipejaner.quotes.error;

import java.util.Map;

public class QuoteValidationException extends QuoteException {
  public QuoteValidationException(Map<String, String> fields) {
    super("VALIDATION_ERROR", "Check the highlighted fields.", fields);
  }
}
