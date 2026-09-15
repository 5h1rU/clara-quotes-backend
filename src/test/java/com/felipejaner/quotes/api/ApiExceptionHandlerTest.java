package com.felipejaner.quotes.api;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class ApiExceptionHandlerTest {
  @Test
  void unexpectedErrorsLogCauseAndStackButReturnGenericMessage(CapturedOutput output) {
    var response =
        new ApiExceptionHandler()
            .unexpected(
                new IllegalStateException(
                    "internal diagnostic", new RuntimeException("root cause")));
    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
    assertThat(response.getBody().message()).doesNotContain("internal diagnostic", "root cause");
    assertThat(output.getAll())
        .contains(
            "IllegalStateException: internal diagnostic",
            "ApiExceptionHandlerTest.java:",
            "Caused by: java.lang.RuntimeException: root cause");
  }
}
