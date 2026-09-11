package com.felipejaner.quotes;

import static org.assertj.core.api.Assertions.*;

import com.felipejaner.quotes.api.CoverageRequest;
import com.felipejaner.quotes.application.CoverageValidator;
import com.felipejaner.quotes.domain.*;
import com.felipejaner.quotes.error.QuoteValidationException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CoverageValidatorTest {
  final CoverageValidator validator = new CoverageValidator();

  @Test
  void youngerApplicantsMustOmitAllHealthFieldsEvenFalseAndEmpty() {
    assertThat(
            validator.validate(
                65, new CoverageRequest(CoverageType.BASIC, null, null, null, null, null)))
        .isNull();
    assertThatThrownBy(
            () ->
                validator.validate(
                    65,
                    new CoverageRequest(CoverageType.BASIC, false, Set.of(), false, false, false)))
        .isInstanceOf(QuoteValidationException.class);
  }

  @Test
  void seniorRequiresAnswersAndConsistentConditions() {
    assertThatThrownBy(
            () ->
                validator.validate(
                    66, new CoverageRequest(CoverageType.BASIC, null, null, null, null, null)))
        .isInstanceOf(QuoteValidationException.class);
    assertThatThrownBy(
            () ->
                validator.validate(
                    66,
                    new CoverageRequest(CoverageType.BASIC, true, Set.of(), false, false, false)))
        .isInstanceOf(QuoteValidationException.class);
    assertThatThrownBy(
            () ->
                validator.validate(
                    66,
                    new CoverageRequest(
                        CoverageType.BASIC, false, Set.of(Condition.OTHER), false, false, false)))
        .isInstanceOf(QuoteValidationException.class);
    assertThat(
            validator
                .validate(
                    66, new CoverageRequest(CoverageType.BASIC, false, Set.of(), true, false, true))
                .needsSpouseCoverage())
        .isTrue();
  }
}
