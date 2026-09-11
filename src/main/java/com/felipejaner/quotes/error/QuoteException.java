package com.felipejaner.quotes.error;


import java.util.Map;
public abstract class QuoteException extends RuntimeException {
    private final String code;
    private final Map<String, String> fields;
    protected QuoteException(String code, String message) { this(code, message, Map.of()); }
    protected QuoteException(String code, String message, Map<String, String> fields) {
        super(message); this.code = code; this.fields = Map.copyOf(fields);
    }
    public String code() { return code; }
    public Map<String, String> fields() { return fields; }
}
