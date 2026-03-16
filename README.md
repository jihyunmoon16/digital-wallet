# Digital Wallet

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
