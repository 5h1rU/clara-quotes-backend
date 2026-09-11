package com.felipejaner.quotes.api;

import com.felipejaner.quotes.error.*;
import java.time.Instant;
import java.util.LinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(QuoteException.class)
  ResponseEntity<ApiError> domain(QuoteException e) {
    HttpStatus status =
        e instanceof QuoteNotFoundException
            ? HttpStatus.NOT_FOUND
            : e instanceof QuoteValidationException
                ? HttpStatus.BAD_REQUEST
                : e instanceof InsurerUnavailableException
                    ? HttpStatus.BAD_GATEWAY
                    : HttpStatus.CONFLICT;
    return ResponseEntity.status(status)
        .body(new ApiError(e.code(), e.getMessage(), e.fields(), Instant.now()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
    var fields = new LinkedHashMap<String, String>();
    e.getBindingResult()
        .getFieldErrors()
        .forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
    return ResponseEntity.badRequest()
        .body(
            new ApiError(
                "VALIDATION_ERROR", "Check the highlighted fields.", fields, Instant.now()));
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<ApiError> malformed(Exception e) {
    return ResponseEntity.badRequest()
        .body(
            ApiError.of(
                "INVALID_REQUEST",
                "Check the request types, enum values, fields, and quote ID. Explicit null health fields are not allowed."));
  }

  @ExceptionHandler(ConcurrencyFailureException.class)
  ResponseEntity<ApiError> conflict(ConcurrencyFailureException e) {
    return ResponseEntity.status(409)
        .body(
            ApiError.of("CONCURRENT_UPDATE", "This quote is being updated. Reload it and retry."));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> unexpected(Exception e) {
    log.error("Unexpected request failure ({})", e.getClass().getSimpleName());
    return ResponseEntity.internalServerError()
        .body(
            ApiError.of("INTERNAL_ERROR", "The request could not be completed. Please try again."));
  }
}
