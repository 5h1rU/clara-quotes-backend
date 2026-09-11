# Implementation plan

Read the complete seven-page assignment before implementation. Keep two private repositories at the candidate's request. Commit working milestones using Felipe Janer's configured identity.

## Sequence
1. Establish the contract and reproducible Java 17/Maven + React/TypeScript builds.
2. Implement a domain model with explicit state transitions, exact decimal pricing, and conditional validation.
3. Add PostgreSQL/Flyway, authenticated REST endpoints, real external HTTP submission, Spring caching, and transactional draft expiration.
4. Persist a submission event in the same transaction as the submitted quote; deliver it to Kafka with a retrying outbox publisher.
5. Implement the three routed frontend steps with Context, React Hook Form, Yup and MUI. The server owns saved quotes; the UI keeps editable inputs and reconciles API responses.
6. Test pricing, age boundaries, state transitions, retries, authentication, submit endpoints, cache eviction, expiration, and UI interactions. Run against PostgreSQL and Kafka locally.
7. Add setup instructions, tradeoffs, transparent AI disclosure, a Java/Spring walkthrough, architecture diagrams, and interview modification exercises.

## Acceptance checklist
- [x] POST /quotes creates DRAFT; GET collection and by id return persisted state.
- [x] PATCH coverage enforces age > 65, rejects health fields at age <= 65, and calculates the exact formula.
- [x] Submit validates completeness, makes a real public HTTP call, records failure, supports retry and idempotent success.
- [x] All application endpoints require authentication; JSON errors and browser CORS work.
- [x] PostgreSQL/JPA, Kafka success event, Spring cache invalidation, one transactional batch expiration.
- [x] Docker Compose starts API + PostgreSQL + Kafka.
- [x] Required unit/integration/component tests and JaCoCo report.
- [x] Responsive three-step real-backend flow and defensive failure handling.
- [x] READMEs explain approach, decisions, AI use, limitations, tests and sibling setup.
- [x] Walkthrough and diagrams accurately match the implemented code.

## Deliberate scope
Use HTTP Basic credentials entered at runtime (never embedded in the browser bundle), local Caffeine caching, and a transactional outbox as the main production-oriented addition. Document the API in a checked-in OpenAPI specification. No real insurer, payment handling or deployment is requested. The public stand-in receives no personal or health data.

Personal data assumption: age 1-120 and US five-digit/ZIP+4 postal codes. Currency display is USD. These are explicit choices because the assignment leaves their formats unspecified.
