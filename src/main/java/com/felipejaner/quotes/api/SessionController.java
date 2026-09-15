package com.felipejaner.quotes.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {
  // Security authenticates the request without loading any quote or personal data.
  @GetMapping("/session")
  ResponseEntity<Void> check() {
    return ResponseEntity.noContent().build();
  }
}
