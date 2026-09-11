package com.felipejaner.quotes.error;

public class InvalidQuoteStateException extends QuoteException {
    public InvalidQuoteStateException(String message) { super("INVALID_QUOTE_STATE", message); }
}
