# Digital Wallet

A backend API simulating a digital wallet system, built with Spring Boot.  
Designed to demonstrate production-level patterns including concurrency control,  
idempotency, structured logging, and cloud deployment.

## Tech Stack

| Category | Technology |
|----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3 |
| Database | PostgreSQL 17 (AWS RDS) |
| Cache | Redis 7 (Docker) |
| ORM | JPA / Hibernate |
| Migration | Flyway |
| Testing | JUnit 5, AssertJ |
| Load Test | k6 |
| Infra | AWS EC2 |

## API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/transfers` | Transfer between accounts |
| GET | `/actuator/health` | Health check |
| GET | `/actuator/health/liveness` | Process liveness probe |
| GET | `/actuator/health/readiness` | Traffic readiness probe |

## Test Code Conventions

### 1) TDD workflow
- Follow `Red -> Green -> Refactor`.
- Add one failing test first.
- Implement the minimum production change to pass.
- Refactor only after tests are green.
- Keep commits behavior-focused (one test rule per commit when possible).

### 2) Test method naming
- Use: `action_withCondition_expectedResult`
- Examples:
  - `deposit_withNonPositiveAmount_throwsIllegalArgumentException`
  - `withdraw_withValidRequest_updatesAccountBalance`

### 3) Test structure
- Use explicit sections in each test:
  - `// given`
  - `// when`
  - `// then`
- If the action and assertion are combined, use `// when & then`.

### 4) Test type split

#### Domain tests
- Purpose: pure domain rule validation.
- Example location: `src/test/java/com/moon/digitalwallet/account/domain`.
- Do not use Spring context annotations here.
- Instantiate domain objects directly (fast unit tests).

#### Service tests
- Purpose: integration behavior with Spring, JPA, transaction, and error mapping.
- Example location: `src/test/java/com/moon/digitalwallet/account/service`.
- Use:
  - `@SpringBootTest`
  - `@Transactional`
  - `@ActiveProfiles("test")`
- Use `@Autowired` real beans (not mocks) for integration scenarios.

### 5) Assertion rules
- Success/state checks: `assertThat(...)`
- Expected exceptions: `assertThatThrownBy(...)`
- Success path with no exception: `assertThatCode(...).doesNotThrowAnyException()`
- For API/domain mapping, validate `BusinessException` and `ErrorCode`.

### 6) Test profile and database
- Integration tests run with `application-test.yml`.
- Test datasource must point to a dedicated DB (`wallet_test`).
- Do not run integration tests against non-test databases.

### 7) Useful commands
- Run all tests:
  - `./gradlew test --console=plain`
- Run one test class:
  - `./gradlew test --tests 'com.moon.digitalwallet.account.domain.AccountTest' --console=plain`
- Run one test method:
  - `./gradlew test --tests 'com.moon.digitalwallet.account.service.AccountServiceTest.withdraw_withValidRequest_updatesAccountBalance' --console=plain`

### 8) Why RuntimeException matters for transaction rollback
- In Spring, `@Transactional` rolls back by default when a `RuntimeException` (or `Error`) is thrown.
- `BusinessException` extends `RuntimeException` on purpose, so business failures automatically trigger rollback.
- This protects consistency in flows like transfer:
  - if `accountFrom.withdraw(amount)` fails, `accountTo.deposit(amount)` is not committed.
  - partial updates are rolled back in the same transaction.
- If you use checked exceptions (`Exception`) for business rules, rollback does not happen by default unless `rollbackFor` is configured.
- Avoid swallowing runtime exceptions inside transactional methods. If you catch one, rethrow a proper business exception so rollback still occurs.

### 9) Why OffsetDateTime instead of LocalDateTime
- `LocalDateTime` has no timezone/offset information, so the same value can mean different instants on different servers.
- `OffsetDateTime` stores both date-time and offset (for example `+09:00`), so the exact instant is explicit.
- This is safer for distributed systems, logs, and audit data where ordering and exact event time matter.
- It also aligns well with PostgreSQL `TIMESTAMPTZ` columns used in this project.
- Rule of thumb:
  - store timestamps with offset (`OffsetDateTime`)
  - convert to local display time only at the API client/UI layer

### 10) Concurrency Control Principles for Transfer
- Domain risk:
  - Transfer is a read-modify-write operation on shared account state.
  - Concurrent requests can pass validation on stale balance and break data integrity.

- Consistency strategy:
  - Use optimistic locking (`@Version`) on `Account` to detect write conflicts.
  - Keep transactional balance updates in `TransferTransactionService`.
  - Keep retry orchestration in `TransferService` (separation of transaction and retry policy).

- Retry and error policy:
  - Retry only optimistic lock conflicts with bounded attempts.
  - Do not retry business rule failures (for example `INSUFFICIENT_BALANCE`).
  - Map unresolved lock conflicts to `CONCURRENT_MODIFICATION` (`409 Conflict`).

- Observable API behavior:
  - Return explicit error code and HTTP status for conflict cases.
  - Include `requestId` in error responses for production traceability.

- Test guarantees:
  - Concurrency integration tests verify invariants:
    - only one transfer succeeds in the competing scenario,
    - final balance remains valid,
    - transfer history increments consistently.
  - Controller tests verify API contracts:
    - error code/status mapping,
    - request id propagation.

### 11) Idempotency Design for Transfer
- Domain risk:
  - Network failures or client timeouts can cause the same transfer request to be sent more than once.
  - Without idempotency, each retry executes a new transfer, resulting in duplicate charges.

- API contract:
  - Every `POST /transfers` request must include an `Idempotency-Key` header with a client-generated UUID.
  - Requests without the header are rejected with `400 Bad Request`.
  - The same key may be safely retried — only the first successful execution takes effect.

- Consistency strategy:
  - Use Redis `SET NX` (`setIfAbsent`) to atomically claim an idempotency key before executing the transfer.
  - Atomicity prevents two concurrent requests with the same key from both proceeding.
  - Store a `requestHash` (`fromAccountId:toAccountId:amount`) alongside the status so key reuse with different parameters is detected and rejected.

- State model:
  - `IN_PROGRESS` — transfer is currently executing. TTL: 30 seconds.
  - `SUCCESS` — transfer completed. Cached `transferId` is returned on retry. TTL: 24 hours.
  - `FAILED` — transfer failed. Key is preserved so the client receives a clear error instead of re-executing a known-bad request. TTL: 5 minutes.

- Redis value format:
  - `IN_PROGRESS|{requestHash}`
  - `SUCCESS|{requestHash}|{transferId}`
  - `FAILED|{requestHash}`

- TTL rationale:
  - `IN_PROGRESS` (30 seconds): covers the maximum expected transfer processing time. Expired keys are automatically released so the client can retry after a timeout.
  - `SUCCESS` (24 hours): covers all realistic retry windows while preventing unbounded memory growth in Redis.
  - `FAILED` (5 minutes): preserves the failure record long enough for the client to detect it, without holding memory indefinitely.

- Error codes:
  - `IDEMPOTENCY_REQUEST_IN_PROGRESS` (`409`) — a request with this key is already being processed.
  - `IDEMPOTENCY_KEY_CONFLICT` (`409`) — the key was previously used with different request parameters.
  - `IDEMPOTENCY_REQUEST_FAILED` (`409`) — the previous request with this key failed; the client must use a new key to retry.

- Why FAILED state instead of deleting the key:
  - Deleting the key on failure allows the same key to be reused immediately, which risks re-executing a request whose failure cause has not been resolved.
  - Storing `FAILED` gives the client a clear signal: this specific request failed, generate a new key for the next attempt.

- Design decisions:
  - Why Redis over a DB-based approach: `setIfAbsent` maps directly to Redis `SET NX`, which is atomic. This prevents race conditions when two identical requests arrive simultaneously — only one acquires the key and proceeds.
  - Why FAILED state instead of deleting the key: storing `FAILED` preserves the failure record for 5 minutes, allowing the client to distinguish between a request that failed and one that was never received. The client is expected to generate a new key and retry rather than reusing a failed key.
  - Why TTL on SUCCESS: Redis is an in-memory store. Without TTL, completed transfer records would accumulate indefinitely. A 24-hour window covers all realistic retry scenarios while keeping memory usage bounded.

- Test guarantees:
  - Idempotency integration tests verify invariants:
    - the same key with the same request executes only once and returns the same `transferId`,
    - the same key with different parameters returns `IDEMPOTENCY_KEY_CONFLICT`,
    - a failed request followed by a retry with the same key returns `IDEMPOTENCY_REQUEST_FAILED`.
  - Controller tests verify API contracts:
    - missing `Idempotency-Key` header returns `400 Bad Request`,
    - successful response includes `transferId` in the response body.

### 12) Health Probe Policy
- Why it matters:
  - A process can be alive while still being unable to serve transfer traffic.
  - This project depends on both PostgreSQL and Redis for correct transfer handling.

- Probe split:
  - `liveness` checks only `ping`.
  - `readiness` checks `db`, `redis`, and `ping`.

- Reason for including Redis in readiness:
  - Transfer requests use Redis-backed idempotency keys.
  - If Redis is unavailable, the system fails closed to avoid duplicate transfers.
  - In that state, the process is alive but should not receive new traffic.

- Operational behavior:
  - `/actuator/health/liveness` answers whether the app process itself is still healthy.
  - `/actuator/health/readiness` answers whether the app can safely accept requests.
  - Production hides component details, while the test profile exposes them so the probe contract can be verified.

- Deployment implication:
  - A load balancer or container platform should use `/actuator/health/readiness` for traffic routing decisions.
  - `liveness` should remain simple so temporary dependency issues do not trigger unnecessary restarts.

### 13) Retry and Timeout Policy
- Why it matters:
  - Retry without timeout can multiply slow failures.
  - Timeout without selective retry can fail too aggressively on short-lived write conflicts.

- Retry policy:
  - Retry only `OptimisticLockingFailureException` in `TransferService`.
  - Maximum attempts: `3`
  - Backoff:
    - after first conflict: `50ms`
    - after second conflict: `100ms`
  - If all attempts fail, return `CONCURRENT_MODIFICATION` (`409 Conflict`).
  - Do not retry business rule failures such as `INSUFFICIENT_BALANCE`.

- Timeout policy:
  - Hikari connection timeout: `3000ms`
  - Hikari validation timeout: `1000ms`
  - Redis command timeout: `1500ms`
  - Transfer transaction timeout: `3s`

- Design rationale:
  - Database and Redis are required dependencies for safe transfer execution.
  - Short infrastructure timeouts prevent threads from being blocked too long during dependency failures.
  - Transfer writes are expected to complete quickly; a transaction that exceeds `3s` is treated as abnormal.
  - Backoff reduces repeated collisions immediately after an optimistic lock conflict.

- Operational rule:
  - Retry only transient technical conflicts.
  - Fail fast on infrastructure slowdown.
  - Return explicit business or conflict errors rather than retrying indiscriminately.

## Deployment

See [DEPLOYMENT.md](DEPLOYMENT.md) for infrastructure setup and deployment instructions.

## Load Test

Performed with [k6](https://k6.io/) against the production environment (EC2 + RDS).

### Scenario

- 200 concurrent virtual users
- 30 seconds duration
- Each user transfers to a unique account pair (no lock contention)

### Results

| Metric | Value |
|--------|-------|
| Total requests | 27,809 |
| Success rate | 100% |
| Throughput | 922 req/s |
| p90 | 252ms |
| p95 | 271ms |
| avg | 214ms |

### Concurrency test

When 200 users target the same account simultaneously, optimistic locking detects conflicts and returns `409 CONCURRENT_MODIFICATION` after 3 retries. This confirms data integrity is maintained under contention.

## Failure Scenarios

### 1. Redis unavailable
- **Symptom**: All transfer requests fail with 500
- **Cause**: `setIfAbsent()` throws `RedisConnectionException`
- **Behavior**: Fail-closed — transfers are rejected until Redis recovers
- **Rationale**: Duplicate transfers are more dangerous than temporary downtime in a financial system
- **Recovery**: Restart Redis, retry failed requests with the same `Idempotency-Key`

### 2. RDS unavailable
- **Symptom**: All transfer requests fail with 500
- **Cause**: HikariCP connection pool exhausted or DB unreachable
- **Behavior**: Spring Boot health check returns `DOWN`
- **Recovery**: RDS automatic failover (if Multi-AZ enabled), or manual restart

### 3. Duplicate transfer request
- **Symptom**: Client retries after network timeout
- **Cause**: Response lost in transit — server processed but client didn't receive
- **Behavior**: Second request hits Redis, finds `SUCCESS` state, returns cached `transferId`
- **Result**: Transfer executes exactly once

### 4. Concurrent transfer conflict
- **Symptom**: `409 CONCURRENT_MODIFICATION`
- **Cause**: Multiple requests modify the same account simultaneously
- **Behavior**: Optimistic lock detects version conflict, retries up to 3 times
- **Result**: One request succeeds, others fail with explicit error code
