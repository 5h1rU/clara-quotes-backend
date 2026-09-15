# Clara Quotes API

Insurance quote onboarding API built with Java 17, Spring Boot 3.5, Maven, PostgreSQL and Kafka. The paired [React frontend](https://github.com/5h1rU/clara-quotes-frontend) calls this API directly. Both repositories are public and can be cloned directly.

## Run both applications

Prerequisites: Docker with Compose, Node.js 22.12+ (or a current supported Node release), and npm. Java is only needed to develop/test the backend outside Docker.

```sh
git clone https://github.com/5h1rU/clara-quotes-backend.git
git clone https://github.com/5h1rU/clara-quotes-frontend.git
cd clara-quotes-backend
cp .env.example .env
docker compose up --build -d
docker compose logs -f api
```

Wait for `Started QuotesApplication`. In another terminal:

```sh
cd clara-quotes-frontend
npm ci
npm run dev
```

Open http://127.0.0.1:5174. Sign in with `reviewer` / `local-review-only`. These are explicit **local demonstration credentials**, configurable using `API_USERNAME` and `API_PASSWORD`. They are never compiled into the frontend bundle. Compose binds its exposed ports to localhost. Use HTTPS and managed credentials before exposing this beyond your machine.

API: http://localhost:8080. PostgreSQL: localhost:5432. Kafka: localhost:9092. The database and broker have named volumes. `docker compose down` preserves them; `docker compose down -v` deletes project data and should only be used for an intentional reset. The one-shot `kafka-volume-init` container gives Kafka's non-root user ownership of its data directory; an exit code of zero is expected.

## Local Java development and tests

Install a JDK **17**. The Maven wrapper supplies Maven:

```sh
docker compose up -d postgres kafka
./mvnw spring-boot:run
./mvnw test                 # fast unit tests; no Docker required
./mvnw verify               # unit tests + PostgreSQL Testcontainers integration tests + JaCoCo
```

Stop the Compose API first (`docker compose stop api`) if running Spring locally on the same port. Testcontainers creates a separate disposable database; it never uses your development quotes. Integration tests stub only the external insurer and Kafka transport, while HTTP controller, security, transactions, Flyway, JPA, cache and PostgreSQL are real. `HttpInsurerGatewayTest` separately exercises real HTTP responses/timeouts against a deterministic loopback server. End-to-end testing uses the actual public stand-in and real broker.

Coverage report: `target/site/jacoco/index.html`. An optional GitHub Actions template is in `docs/ci/github-actions.yml`; it runs verification and uploads reports. It is not installed as a workflow because the current GitHub OAuth token lacks the workflow scope. Move it to `.github/workflows/verify.yml` using credentials with that permission if enabling CI. `./mvnw spotless:apply` formats Java; `./mvnw spotless:check` checks formatting.

On this Mac, Java and Docker were installed with Homebrew and Colima:

```sh
brew install openjdk@17 maven colima docker docker-compose
colima start --cpu 4 --memory 6
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export DOCKER_HOST=unix://$HOME/.colima/default/docker.sock
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
./mvnw verify
```

If `docker compose` is unavailable after Homebrew installation, link `/opt/homebrew/lib/docker/cli-plugins/docker-compose` into `~/.docker/cli-plugins/`. Linux and Docker Desktop normally need none of these Colima overrides.

## Approach before implementation

I first mapped the seven-page brief to a state machine, API boundaries, and failure cases; the original plan is in [docs/PLAN.md](docs/PLAN.md). The difficult part is not multiplying prices. It is preserving the correct state when HTTP fails, preventing two concurrent submissions, and avoiding a database/Kafka split outcome. I built and tested the backend contract first, then connected the three-step UI, and finally exercised the applications together.

The implementation keeps classes small enough to explain in an interview. It uses explicit constructors and ordinary Java, without Lombok, reflection-based mapping libraries, a generic rules engine, or unnecessary service interfaces.

## Contract

All application endpoints require an explicit HTTP Basic `Authorization` header. CORS preflight OPTIONS requests are the necessary browser exception. Input names and enum values follow the assignment. See [OpenAPI](docs/openapi.yaml) for full payloads and [curl examples](docs/API-EXAMPLES.md).

| Method | Path | Behavior |
| --- | --- | --- |
| GET | `/session` | Check credentials; return 204 without loading quotes |
| POST | `/quotes` | Validate personal data, create DRAFT, return 201 and Location |
| GET | `/quotes` | Return quotes, newest first; empty collection is `[]` |
| GET | `/quotes/{id}` | Return full state, cached locally |
| PATCH | `/quotes/{id}/coverage` | Validate conditional health fields and return recalculated premium |
| POST | `/quotes/{id}/submit` | Submit complete quote; repeated success is idempotent |

`400` means invalid input, `401` invalid/missing credentials, `404` missing quote or route, `405` unsupported method, `415` unsupported content type, `409` invalid state/concurrent update, and `502` insurer failure. Errors have `{code, message, fieldErrors, timestamp}` and never expose stack traces. Unexpected failures log the full exception on the server. Framework errors retain their status and headers, including `Allow` for 405.

At age **65**, health properties must be omitted altogether, including empty arrays, `false`, and explicit `null`. At age **66**, all four yes/no answers are required. A true conditions answer requires one or more conditions; a false answer requires no selected conditions. Multiple conditions apply the multiplier once. Medication is recorded but has **no pricing multiplier**. The exact example is `$100 × 1.5 × 1.3 × 1.2 × 1.4 = $327.60`.

The brief does not specify age/ZIP formats or currency. This implementation explicitly chooses age 1-120, US ZIP/ZIP+4, and USD. All coverage levels share the same completeness requirements because no other coverage-specific fields were specified.

## Design decisions

- **Domain state machine:** `Quote` controls edit/submit transitions. DRAFT and SUBMISSION_FAILED are editable; SUBMITTED and EXPIRED are terminal. Submission of SUBMITTED returns the existing response before making any side effect.
- **Shared age rule:** `ApplicantRules.isSenior` owns the backend age boundary for validation, submission completeness and pricing. The frontend mirrors that predicate in `applicantRules.ts` for forms, summary and preview. Both runtimes enforce the fixed age-65/66 contract independently.
- **Composed pricing strategies:** `PremiumCalculator` combines `PremiumFactor` functions and `BigDecimal`, rounding once to two decimal places. Pricing is independent of HTTP and persistence. New multipliers can be added without modifying submission logic. This is deliberately a small composition, not a rule-engine framework.
- **Ports and adapters:** `SubmissionService` depends on the `InsurerGateway` interface; `HttpInsurerGateway` implements it with Java's HTTP client. Tests replace the boundary without changing business logic. Constructor injection makes dependencies visible.
- **Concurrency:** a PostgreSQL pessimistic row lock serializes mutation of one quote. A bounded external request runs while that lock is held. This is an intentional take-home tradeoff: simple, testable concurrency at the cost of holding a database connection during network latency. A higher-throughput design would use an intermediate submission state, a lease, and asynchronous completion.
- **Failure transaction:** `noRollbackFor = InsurerUnavailableException.class` allows the database to commit SUBMISSION_FAILED even when the API returns 502. Unexpected errors still roll back. The quote remains retryable.
- **Transactional outbox (production-oriented addition):** quote success and one unique event are saved in the same PostgreSQL transaction. A scheduled publisher sends pending events to Kafka and records acknowledgement. Kafka outages do not lose the event or force a second insurer call. Delivery is **at least once**; consumers must deduplicate by `eventId`. This does not claim distributed exactly-once delivery.
- **Cache:** `QuoteCache` uses Spring's `Cache` abstraction backed by Caffeine to cache immutable response DTOs. Cache keys contain the quote ID and a local generation number. An AFTER_COMMIT listener advances that generation and clears the cache whenever a quote changes, including failed submissions and batch expiration. An older in-flight read can finish with its original snapshot, but any late cache fill uses an obsolete generation and cannot be served to later requests. Clearing all entries simplifies correctness at the cost of unrelated cache misses. Capacity is 1,000 and TTL is 30 seconds. This is a single-instance design; multiple instances require coordinated invalidation.
- **Expiration:** one JPQL bulk UPDATE finds DRAFT quotes older than `DRAFT_TTL`, changes status and version together, then clears the cache. Age is measured from creation, not last editing. Failed submissions are intentionally excluded, as the assignment specifies DRAFT.
- **Database migrations:** Flyway owns schema changes; Hibernate validates the schema instead of silently editing it on startup.
- **OpenAPI (second addition):** a checked-in specification makes the two-repository contract reviewable without requiring a running UI. It is documentation, not an unauthenticated Swagger endpoint.

The external stand-in is a real GET to `https://httpbin.org/status/200`, configured through `INSURER_URL`, with a five-second default timeout. Only a quote UUID is sent as `Idempotency-Key`; **no personal or health data leaves for the public stand-in**. It emulates a result, not an actual insurer. Its endpoint does not guarantee idempotency: a process crash after remote acceptance but before database commit can repeat the call. A real provider would need an idempotency contract and reconciliation. There is no hidden automatic insurer retry.

## Configuration

See `.env.example`. Compose reads that file; a locally run JVM reads exported environment variables (it does not automatically load `.env`). Important settings: `INSURER_URL`, `INSURER_TIMEOUT`, `DRAFT_TTL`, `DRAFT_EXPIRATION_INTERVAL`, `FRONTEND_ORIGIN`, database and API credentials. The outbox polls every three seconds by default. The Kafka topic is `quotes.submitted.v1`.

To demonstrate a genuine external error, restart just the API with `INSURER_URL=https://httpbin.org/status/503 docker compose up -d --force-recreate api`. Submit a complete quote, observe 502 and SUBMISSION_FAILED, then restore the URL to `/status/200`, recreate the API, and retry the **same** quote. Do not set an override simultaneously in `.env` and the shell unless you intend the shell value to win.

Consume a published event:

```sh
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh   --bootstrap-server kafka:19092 --topic quotes.submitted.v1   --from-beginning --max-messages 1 --timeout-ms 15000
```

## AI use, challenges and limits

AI assistance (OpenAI Codex) was used to read the brief, propose the implementation plan, generate and revise code/tests/docs, set up the local runtime, and execute validation. Felipe directed the required stack, incremental commit ownership, and interview-learning materials. Automated checks and actual browser/API tests were used to review generated output. The walkthrough and interview exercises support Felipe’s ongoing manual code review. Commits use Felipe's Git identity, with no AI co-author trailer.

Validation uncovered and fixed absent-vs-null JSON handling, CORS preflight wiring, an ARM-incompatible runtime image, and Kafka data-volume ownership. See [verification evidence](docs/VERIFICATION.md) for actual checks and outcomes.

Remaining scope limits: one shared reviewer account (no customer ownership/roles), unpaginated list per the brief (conditions fetched in the same query; sign-in uses `/session`), local cache, no insurer crash reconciliation, no outbox retention/dead-letter administration, and no public deployment. Broker delivery is asynchronous; SUBMITTED means insurer accepted and the event is durably pending or delivered. Public free APIs can be unavailable. These are documented tradeoffs, not guarantees of production readiness.

Start learning with [the Java/Spring walkthrough](docs/WALKTHROUGH.md), then use [interview exercises](docs/INTERVIEW.md).
