package com.felipejaner.quotes;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.felipejaner.quotes.api.*;
import com.felipejaner.quotes.application.*;
import com.felipejaner.quotes.error.InsurerUnavailableException;
import com.felipejaner.quotes.messaging.*;
import com.felipejaner.quotes.submission.InsurerGateway;
import jakarta.persistence.EntityManagerFactory;
import java.util.UUID;
import java.util.concurrent.*;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers
@SpringBootTest(
    properties = {
      "quotes.scheduling.enabled=false",
      "spring.kafka.admin.auto-create=false",
      "spring.jpa.properties.hibernate.generate_statistics=true",
      "logging.level.org.hibernate.stat=OFF",
      "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF"
    })
@AutoConfigureMockMvc
class QuoteApiIT {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    r.add("spring.datasource.username", postgres::getUsername);
    r.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired JdbcTemplate jdbc;
  @Autowired QuoteService quotes;
  @Autowired DraftExpirationService expiration;
  @Autowired CacheManager cache;
  @Autowired OutboxPublisher publisher;
  @Autowired EntityManagerFactory entityManagerFactory;
  @MockitoBean InsurerGateway insurer;
  @MockitoBean KafkaTemplate<String, String> kafka;

  @BeforeEach
  void clean() {
    jdbc.update("delete from outbox_events");
    jdbc.update("delete from quote_conditions");
    jdbc.update("delete from quotes");
    cache.getCache("quotes").clear();
  }

  UUID create(int age) throws Exception {
    var result =
        mvc.perform(
                post("/quotes")
                    .with(httpBasic("reviewer", "local-review-only"))
                    .contentType("application/json")
                    .content(
                        "{\"name\":\"Test Person\",\"email\":\"test@example.com\",\"age\":"
                            + age
                            + ",\"zipCode\":\"90210\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andReturn();
    return UUID.fromString(
        mapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
  }

  void coverage(UUID id) throws Exception {
    mvc.perform(
            patch("/quotes/" + id + "/coverage")
                .with(httpBasic("reviewer", "local-review-only"))
                .contentType("application/json")
                .content("{\"coverageType\":\"STANDARD\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estimatedMonthlyPremium").value(100));
  }

  @Test
  void frameworkErrorsKeepTheirStatusHeadersAndApiShape() throws Exception {
    mvc.perform(get("/unknown-path").with(httpBasic("reviewer", "local-review-only")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
        .andExpect(jsonPath("$.fieldErrors").isMap());
    mvc.perform(delete("/quotes").with(httpBasic("reviewer", "local-review-only")))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(header().exists("Allow"))
        .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    mvc.perform(
            post("/quotes")
                .with(httpBasic("reviewer", "local-review-only"))
                .contentType("text/plain")
                .content("invalid"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
  }

  @Test
  void sessionCheckAuthenticatesWithoutReadingQuotesAndListFetchesConditionsOnce()
      throws Exception {
    create(30);
    create(70);
    var stats = entityManagerFactory.unwrap(SessionFactoryImplementor.class).getStatistics();
    stats.clear();
    mvc.perform(get("/session")).andExpect(status().isUnauthorized());
    mvc.perform(get("/session").with(httpBasic("reviewer", "local-review-only")))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));
    assertThat(stats.getPrepareStatementCount()).isZero();
    mvc.perform(get("/quotes").with(httpBasic("reviewer", "local-review-only")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
    assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
  }

  @Test
  void authenticatesEveryEndpointAndAllowsBrowserPreflight() throws Exception {
    mvc.perform(get("/quotes"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    mvc.perform(post("/quotes")).andExpect(status().isUnauthorized());
    mvc.perform(get("/quotes/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
    mvc.perform(patch("/quotes/" + UUID.randomUUID() + "/coverage"))
        .andExpect(status().isUnauthorized());
    mvc.perform(post("/quotes/" + UUID.randomUUID() + "/submit"))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            options("/quotes")
                .header("Origin", "http://127.0.0.1:5174")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5174"));
  }

  @Test
  void validatesPersonalDataUnknownFieldsAndMissingQuote() throws Exception {
    mvc.perform(
            post("/quotes")
                .with(httpBasic("reviewer", "local-review-only"))
                .contentType("application/json")
                .content("{\"name\":\"\",\"email\":\"bad\",\"age\":0,\"zipCode\":\"x\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors.email").exists());
    mvc.perform(
            get("/quotes/" + UUID.randomUUID()).with(httpBasic("reviewer", "local-review-only")))
        .andExpect(status().isNotFound());
    var id = create(65);
    for (String fields :
        new String[] {
          "\"usesTobacco\":false", "\"usesTobacco\":null", "\"conditions\":[]", "\"unknown\":true"
        })
      mvc.perform(
              patch("/quotes/" + id + "/coverage")
                  .with(httpBasic("reviewer", "local-review-only"))
                  .contentType("application/json")
                  .content("{\"coverageType\":\"BASIC\"," + fields + "}"))
          .andExpect(status().isBadRequest());
  }

  @Test
  void seniorFormulaAndConditionalValidation() throws Exception {
    var id = create(70);
    mvc.perform(
            patch("/quotes/" + id + "/coverage")
                .with(httpBasic("reviewer", "local-review-only"))
                .contentType("application/json")
                .content("{\"coverageType\":\"STANDARD\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            patch("/quotes/" + id + "/coverage")
                .with(httpBasic("reviewer", "local-review-only"))
                .contentType("application/json")
                .content(
                    "{\"coverageType\":\"STANDARD\",\"hasPreexistingConditions\":true,\"conditions\":[\"DIABETES\"],\"takesPrescriptionMedication\":true,\"usesTobacco\":true,\"needsSpouseCoverage\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estimatedMonthlyPremium").value(327.60));
  }

  @Test
  void failureCommitsThenRetrySucceedsAndIsIdempotent() throws Exception {
    var id = create(30);
    coverage(id);
    quotes.get(id); // Prime cache before status changes.
    doThrow(new InsurerUnavailableException("Insurer is unavailable. Retry."))
        .doNothing()
        .when(insurer)
        .submit(id);
    mvc.perform(post("/quotes/" + id + "/submit").with(httpBasic("reviewer", "local-review-only")))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.code").value("INSURER_UNAVAILABLE"));
    assertThat(quotes.get(id).status().name()).isEqualTo("SUBMISSION_FAILED");
    for (int i = 0; i < 2; i++)
      mvc.perform(
              post("/quotes/" + id + "/submit").with(httpBasic("reviewer", "local-review-only")))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("SUBMITTED"));
    verify(insurer, times(2)).submit(id);
    assertThat(quotes.get(id).status().name()).isEqualTo("SUBMITTED");
    assertThat(jdbc.queryForObject("select count(*) from outbox_events", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void cachedReadChangesAfterCoverageAndBatchExpiration() throws Exception {
    var id = create(30);
    assertThat(quotes.get(id).coverageType()).isNull();
    coverage(id);
    assertThat(quotes.get(id).estimatedMonthlyPremium()).isEqualByComparingTo("100.00");
    jdbc.update("update quotes set created_at = now() - interval '31 minutes' where id = ?", id);
    var fresh = create(40);
    assertThat(expiration.expire()).isEqualTo(1);
    assertThat(quotes.get(id).status().name()).isEqualTo("EXPIRED");
    assertThat(quotes.get(fresh).status().name()).isEqualTo("DRAFT");
    mvc.perform(post("/quotes/" + id + "/submit").with(httpBasic("reviewer", "local-review-only")))
        .andExpect(status().isConflict());
    verifyNoInteractions(insurer);
  }

  @Test
  void expirationUsesDraftAgeAndKeepsFailedSubmissionsRetryable() throws Exception {
    var editedDraft = create(30);
    coverage(editedDraft);
    var failed = create(30);
    coverage(failed);
    doThrow(new InsurerUnavailableException("offline")).when(insurer).submit(failed);
    mvc.perform(
            post("/quotes/" + failed + "/submit").with(httpBasic("reviewer", "local-review-only")))
        .andExpect(status().isBadGateway());
    jdbc.update("update quotes set created_at = now() - interval '31 minutes'");
    assertThat(expiration.expire()).isEqualTo(1);
    assertThat(quotes.get(editedDraft).status().name()).isEqualTo("EXPIRED");
    assertThat(quotes.get(failed).status().name()).isEqualTo("SUBMISSION_FAILED");
  }

  @Test
  void incompleteQuoteCannotSubmit() throws Exception {
    var id = create(30);
    mvc.perform(post("/quotes/" + id + "/submit").with(httpBasic("reviewer", "local-review-only")))
        .andExpect(status().isConflict());
    verifyNoInteractions(insurer);
  }

  @Test
  void concurrentSubmissionsCallInsurerOnce() throws Exception {
    var id = create(30);
    coverage(id);
    doAnswer(
            inv -> {
              Thread.sleep(150);
              return null;
            })
        .when(insurer)
        .submit(id);
    var executor = Executors.newFixedThreadPool(2);
    try {
      Callable<Integer> call =
          () ->
              mvc.perform(
                      post("/quotes/" + id + "/submit")
                          .with(httpBasic("reviewer", "local-review-only")))
                  .andReturn()
                  .getResponse()
                  .getStatus();
      var first = executor.submit(call);
      var second = executor.submit(call);
      assertThat(first.get(10, TimeUnit.SECONDS)).isEqualTo(200);
      assertThat(second.get(10, TimeUnit.SECONDS)).isEqualTo(200);
      verify(insurer, times(1)).submit(id);
      assertThat(jdbc.queryForObject("select count(*) from outbox_events", Integer.class))
          .isEqualTo(1);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void repeatedReadUsesCacheUntilQuoteChanges() throws Exception {
    var id = create(30);
    quotes.get(id);
    var stats = entityManagerFactory.unwrap(SessionFactoryImplementor.class).getStatistics();
    stats.clear();
    quotes.get(id);
    assertThat(stats.getPrepareStatementCount()).isZero();
    coverage(id);
    assertThat(quotes.get(id).estimatedMonthlyPremium()).isEqualByComparingTo("100.00");
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void synchronousBrokerFailureCommitsEarlierAcknowledgementAndRetriesPending(boolean spring)
      throws Exception {
    for (int i = 0; i < 2; i++) {
      var id = create(30);
      coverage(id);
      mvc.perform(
              post("/quotes/" + id + "/submit").with(httpBasic("reviewer", "local-review-only")))
          .andExpect(status().isOk());
    }
    RuntimeException failure =
        spring
            ? new org.springframework.kafka.KafkaException("send failed")
            : new org.apache.kafka.common.KafkaException("send failed");
    when(kafka.send(anyString(), anyString(), anyString()))
        .thenReturn(CompletableFuture.completedFuture(null))
        .thenThrow(failure)
        .thenReturn(CompletableFuture.completedFuture(null));
    publisher.publishPending();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from outbox_events where published_at is not null", Integer.class))
        .isEqualTo(1);
    publisher.publishPending();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from outbox_events where published_at is not null", Integer.class))
        .isEqualTo(2);
    verify(kafka, times(3)).send(anyString(), anyString(), anyString());
  }

  @Test
  void outboxRetriesAfterBrokerFailure() throws Exception {
    var id = create(30);
    coverage(id);
    mvc.perform(post("/quotes/" + id + "/submit").with(httpBasic("reviewer", "local-review-only")))
        .andExpect(status().isOk());
    when(kafka.send(anyString(), anyString(), anyString()))
        .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("offline")))
        .thenReturn(CompletableFuture.completedFuture(null));
    publisher.publishPending();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from outbox_events where published_at is null", Integer.class))
        .isEqualTo(1);
    publisher.publishPending();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from outbox_events where published_at is null", Integer.class))
        .isZero();
    verify(kafka, times(2))
        .send(eq("quotes.submitted.v1"), eq(id.toString()), contains("QuoteSubmitted"));
  }
}
