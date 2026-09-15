# Verification record

## Review follow-up, 2026-09-15

- `./mvnw spotless:apply verify`: passed, 13 unit tests and 14 PostgreSQL integration cases, zero failures/errors/skips. `./mvnw spotless:check` also passed.
- JaCoCo: 93.8% line coverage and 85.3% branch coverage.
- Rebuilt only the local API container, preserving its existing public insurer `/status/503` override. Live authenticated probes returned `/session` 204 with no body, unknown path 404, unsupported DELETE 405 with `Allow: GET, POST`, and wrong content type 415. Existing PostgreSQL and Kafka containers/volumes were preserved.
- Added regressions for synchronous Spring and Apache Kafka exceptions after an earlier acknowledgement in the same outbox batch. The earlier acknowledgement commits and only the remaining event retries.
- MockMvc verifies framework 404/405/415 statuses, the API error shape and the 405 `Allow` header. A captured-log test verifies internal exception messages, stack frames and root causes stay in server logs while the response remains generic.
- A controlled concurrent cache test pauses an old read, invalidates the cache, stores a fresh result and then releases the old read. Later requests still get the fresh result. Both quote-specific and bulk invalidation events are covered.
- Hibernate statement counts verify `/session` reads no quotes, collection GET fetches both quotes and conditions in one query, and repeated quote reads hit the cache. Integration tests verify refresh after coverage, submission failure, success and expiration.
- The expiration regression confirms the chosen policy: elapsed time since creation for DRAFT; recent editing does not reset the timer; SUBMISSION_FAILED stays retryable.

The submission row lock remains a documented throughput trade-off. The brief explicitly asks for all quotes and DRAFT expiration; those contracts remain intact. This follow-up does not claim crash recovery, multiple-instance cache consistency or load testing.

## Original verification, 2026-09-11

Executed locally on 2026-09-11, macOS Apple Silicon, JDK 17, Maven wrapper, Colima/Docker. Only synthetic personal data was used.

| Check | Outcome |
| --- | --- |
| `./mvnw spotless:check verify` | Passed: 10 unit tests and 8 PostgreSQL integration tests, zero failures/errors/skips |
| JaCoCo | 91.9% line coverage; 84.7% branch coverage after formatting |
| `docker compose up -d --build` | API, PostgreSQL 17 and Kafka 4.0.2 running; database/broker healthy |
| Unauthenticated GET /quotes | JSON 401 |
| Age 65, omitted health fields | Accepted; no age multiplier |
| Age 65, false/empty/null health fields | Rejected by API tests |
| Age 70 example | Server and browser return 327.60 |
| Real public `/status/503` call | HTTP 502 from this API; GET confirms persisted SUBMISSION_FAILED |
| Restore public `/status/200`, same quote ID | Retry returns 200 SUBMITTED |
| Repeat successful submission | 200 with same ID; integration test verifies no repeated insurer call/event |
| Kafka console consumer | Read a real QuoteSubmitted event with ID, quote ID, price, version and timestamp |
| Outbox acknowledgement | PostgreSQL confirms delivered event timestamps |
| Parallel submissions | Real PostgreSQL integration test confirms one insurer call and one outbox event |
| Expiration and cache | Integration test confirms one transactional update, cache invalidation and rejected expired submission |

The live retry quote was `d983443a-65c4-4ebf-adcf-b2a8ea9f17af`. The first consumed Kafka event was `c90b33f3-4979-4f62-bad9-00c1f1e6d4b9` for quote `6f4dd8a7-8712-452c-8811-f6334b41473f`, premium 327.60. These are synthetic test records in the local database, not fixtures needed to run the app.

The deterministic automated suite mocks remote insurer and Kafka boundaries; separate live checks verified genuine public HTTP and actual Kafka delivery. The draft-expiration service is tested directly against PostgreSQL; the record does not claim a separate 30-minute wall-clock expiration test. No hosted deployment, load test, multi-node cache test, or crash-recovery guarantee is claimed.

GitHub Actions were not run: pushing workflow files was rejected because the existing OAuth login lacks workflow scope. The configuration is preserved as an optional template in `docs/ci/`. No account permissions were changed.
