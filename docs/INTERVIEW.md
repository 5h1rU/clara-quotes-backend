# Interview preparation

Do these exercises on a temporary local branch. Keep the assignment's fixed behavior on main. The goal is to explain and modify the actual code, including where it remains limited.

## A two-minute explanation to practice

“I split the workflow into a persisted quote state machine and three forms. The server validates conditional health inputs and calculates prices using decimal arithmetic. Mutations lock the quote row so concurrent submits cannot trigger two successful submissions. An expected upstream failure commits a retryable failed state. Success saves an outbox event in the same transaction as the quote; a worker publishes it to Kafka. React previews prices immediately, but the review page uses the saved server response. Tests target the age boundary, failure transactions, duplicate calls, cache eviction and expiration.”

Say this in your own words and then open the classes. Be explicit that AI assisted the implementation and that your preparation is to understand and own its decisions. Do not claim knowledge you have not practiced.

## Questions worth being able to answer

| Question | Find it / explain it |
| --- | --- |
| Where does a request enter? | Security filter first, then `QuoteController`; validation can reject before the method body. |
| Why use an interface for the insurer? | `InsurerGateway` separates business intent from HTTP transport and allows controlled boundary tests. |
| Why no interface for every service? | There is no second implementation or external boundary requiring it; extra layers would obscure the flow. |
| Why BigDecimal? | Exact decimal money, decimal-string factors, single final rounding. |
| What happens at 65 versus 66? | Health forbidden versus required; false and empty arrays still count as supplied data. |
| Why is CoverageRequest not a record? | Setters preserve omitted-versus-explicit-null behavior with Jackson. |
| Why does a failed submit need noRollbackFor? | Otherwise the exception rolls back the status change and loses the failure record. |
| How is duplicate submit handled? | Locked row plus early SUBMITTED return; unique outbox quote ID adds a database invariant. |
| What if two clients submit at once? | Second transaction waits, then sees success; tested with concurrent MockMvc requests. |
| Is it exactly once? | No. The outbox is at least once; provider acceptance before DB commit and broker ack before marking published remain crash windows. |
| What if Kafka is down? | The quote stays submitted with a pending event; publisher retries without calling the insurer again. |
| Why not just publish after saving? | Database commit and Kafka publish are separate systems; a crash can lose the event. |
| What is JPA dirty checking? | Managed entities changed inside a transaction are flushed without a separate save. |
| What does @Version do? | Detects stale entity updates; the bulk expiration query increments it explicitly. |
| Why a single expiration UPDATE? | Atomic batch transition, less round-trip overhead, no partial per-row job progress. |
| Why AFTER_COMMIT eviction? | Invalidate according to committed state, including expected failed submissions. |
| Is the cache strictly consistent? | Late fills cannot revive an invalidated generation. In-flight reads can still return their original snapshot; multiple instances need coordinated invalidation. Mutation decisions bypass the cache. |
| Why does this.method() matter? | Self-invocation bypasses Spring's proxy and its transaction/cache advice. |
| Why not save credentials in localStorage? | It unnecessarily persists reusable secrets; memory suffices for this local reviewer flow. |
| Why does editing personal info create a quote? | The fixed API has no personal-update route; document the tradeoff and expire old drafts. |
| Which SOLID ideas are concrete here? | Single responsibilities, composed pricing factors, substitutable insurer port, a small focused interface, and constructor dependency inversion. |

## Exercise 1: change the expiration window (5 minutes)

Change `DRAFT_TTL` to two minutes through configuration, restart the API, create a draft, wait for the configured scheduled interval, and read it again. Explain why changing `application.yml` defaults differs from setting an environment variable. Do not change the status manually. Restore 30 minutes afterward.

## Exercise 2: add a condition label (10 minutes)

Hypothetically add ASTHMA. Locate the Java enum, TypeScript union/options, OpenAPI schema, tests and summary. Explain why the multiplier stays 1.3 even with two selected conditions and whether this enum addition requires a database migration with the current schema. Do not leave it on main; the assignment enumerates the permitted choices.

## Exercise 3: reject whitespace-only names (5 minutes)

Find the existing `@NotBlank` and Yup `trim().required()` protections. Write one failing-input test rather than adding a duplicate rule. Explain client convenience versus server enforcement.

## Exercise 4: modify a pricing rule (15 minutes)

On a practice branch only, change the tobacco multiplier from 1.2 to 1.25. Predict the reference example: 341.25. Update both preview and backend tests before code. Notice that the frontend's integer factor scale of 10 must become 100 to represent 1.25; updating one number alone is not enough. Restore the fixed assignment formula afterward.

## Exercise 5: simulate Kafka outage (15 minutes)

Stop Kafka, submit a quote, inspect a pending outbox row, restart Kafka and consume the event. Explain why the browser can show submitted while Kafka has not yet acknowledged it. Check the same event ID and discuss deduplication. Use synthetic data only.

## Exercise 6: show an actual external failure (10 minutes)

Use the README's `/status/503` override. Submit, confirm the error and persisted SUBMISSION_FAILED using GET, restore `/status/200`, then retry the same UUID. Explain why the HTTP client does not blindly retry the request itself.

## Exercise 7: find a production limit (10 minutes)

Choose one: holding a row lock during a network call, shared authentication with no row ownership, cache invalidation across instances, unpaginated lists, or outbox growth. Propose the smallest coherent improvement and name the tests and migrations it needs. Do not respond “add microservices” without explaining the actual failure being addressed.

## When asked to make a live change

Restate the behavior and edge cases. Find the existing test and run it. Add or adjust an assertion, modify the smallest responsible layer, and rerun the focused check. Explain if the API contract, database schema or frontend must change too. Finish by naming what you have verified and what you have not.
