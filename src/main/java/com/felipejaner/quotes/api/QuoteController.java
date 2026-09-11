package com.felipejaner.quotes.api;


import com.felipejaner.quotes.application.QuoteService;
import com.felipejaner.quotes.submission.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/quotes")
public class QuoteController {
    private final QuoteService quotes;
    private final SubmissionService submissions;
    public QuoteController(QuoteService quotes, SubmissionService submissions) { this.quotes = quotes; this.submissions = submissions; }
    @PostMapping
    ResponseEntity<QuoteResponse> create(@Valid @RequestBody CreateQuoteRequest request) {
        var quote = quotes.create(request);
        return ResponseEntity.created(URI.create("/quotes/" + quote.id())).body(quote);
    }
    @GetMapping List<QuoteResponse> list() { return quotes.list(); }
    @GetMapping("/{id}") QuoteResponse get(@PathVariable UUID id) { return quotes.get(id); }
    @PatchMapping("/{id}/coverage") QuoteResponse coverage(@PathVariable UUID id, @Valid @RequestBody CoverageRequest request) {
        return quotes.updateCoverage(id, request);
    }
    @PostMapping("/{id}/submit") QuoteResponse submit(@PathVariable UUID id) { return submissions.submit(id); }
}
