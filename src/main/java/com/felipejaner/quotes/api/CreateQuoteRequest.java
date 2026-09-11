package com.felipejaner.quotes.api;

import jakarta.validation.constraints.*;

public record CreateQuoteRequest(
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Email @Size(max = 254) String email,
    @NotNull @Min(1) @Max(120) Integer age,
    @NotBlank
        @Pattern(regexp = "[0-9]{5}(-[0-9]{4})?", message = "Use a five-digit US ZIP or ZIP+4.")
        String zipCode) {}
