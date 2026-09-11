# API examples

These commands use the local demonstration account. Replace credentials if you configured your own.

```sh
curl -i -u reviewer:local-review-only -H 'Content-Type: application/json'   -d '{"name":"Example Person","email":"example@example.com","age":70,"zipCode":"90210"}'   http://localhost:8080/quotes
```

Copy the returned UUID into `QUOTE_ID`:

```sh
QUOTE_ID=the-returned-uuid
curl -s -u reviewer:local-review-only -X PATCH -H 'Content-Type: application/json'   -d '{"coverageType":"STANDARD","hasPreexistingConditions":true,"conditions":["DIABETES"],"takesPrescriptionMedication":true,"usesTobacco":true,"needsSpouseCoverage":true}'   "http://localhost:8080/quotes/$QUOTE_ID/coverage"
curl -i -u reviewer:local-review-only -X POST "http://localhost:8080/quotes/$QUOTE_ID/submit"
curl -s -u reviewer:local-review-only "http://localhost:8080/quotes/$QUOTE_ID"
```

Coverage returns `estimatedMonthlyPremium: 327.60` (JSON clients may display 327.6). The first successful submit produces SUBMITTED and one outbox event. Submit again to verify the same state and no new event. A quote aged 65 or younger sends only `{"coverageType":"STANDARD"}`. Missing auth returns 401; missing coverage returns 409.
