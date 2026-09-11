package com.felipejaner.quotes.error;

public class QuoteNotFoundException extends QuoteException {
    public QuoteNotFoundException(String message) { super("QUOTE_NOT_FOUND", message); }
}
