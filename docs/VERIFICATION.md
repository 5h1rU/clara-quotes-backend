# Verification record

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
