package com.felipejaner.quotes.error;

public class InsurerUnavailableException extends QuoteException {
  public InsurerUnavailableException(String message) {
    super("INSURER_UNAVAILABLE", message);
  }
}
