# Task API, built with Claude Code

A REST API for tasks owned by users: create, read, update and delete tasks with a title, description, status and due
date, behind JWT authentication. Java 25, Spring Boot 4.1, PostgreSQL 17, 151 tests.

This README covers the prompt, how the work was refined before any code, a sample of the output, how the AI's
suggestions were validated and corrected, and how edge cases, authentication and validation were handled.

## Run

Docker only:

```bash
cd task-api
docker compose up --build
```

The API listens on http://localhost:8090, with Swagger UI at `/swagger-ui/index.html`. Ports are off the defaults
(8090, and 5433 for PostgreSQL) so it does not collide with other local services; change them with `API_PORT` and
`DB_PORT`.

| Demo user | Password |
| --- | --- |
| `alice` | `wonderland123` |
| `bob` | `builder12345` |

Tests (Docker must be running, for Testcontainers):

```bash
cd task-api
./gradlew build
```

## 1. The prompt

[PROMPT.md](PROMPT.md). What makes it work:

- **Rules stated, gaps flagged.** Everything already decided is written down. What was not decided (status changes,
  past due dates, update semantics, delete) is listed, and the tool must ask instead of guessing. Unstated rules
  otherwise turn into framework defaults.
- **Explicit engineering expectations.** Separate sections for structure, design patterns, clean code and TDD, each
  specific enough to check in the output.
- **A working agreement.** No code in the first answer; one slice at a time, test-first, stopping for review; never
  claim a result without running it; never commit, because I review and commit every change.
- **A definition of done** I can verify myself.

This is the final version of the prompt. Two parts came from reviewing the first run: the package structure (the first
version asked for `domain`, `application`, `infrastructure` and `web`) and the rule against committing.

## 2. Refinement before code

The tool's first answer had no code: six questions, each with a recommendation, seven assumptions, and a plan with
tables, endpoints, packages and the tests for each of eight slices. My decisions:

| Question | Decision |
| --- | --- |
| Status changes | TODO → IN_PROGRESS or DONE; IN_PROGRESS → TODO or DONE; DONE → IN_PROGRESS (reopen). DONE → TODO refused with 422. The current status again is a no-op |
| Past due date | Refused on create; on update only when the date changes, so an overdue task stays editable. "Today" is the UTC date |
| Update | `PUT` replaces title, description and due date; `PATCH /{id}/status` is the only way to change status |
| Delete | Hard delete; `DELETE /{id}?version=` (no body), 409 when stale, 404 the second time |
| Refused transition | 422, so 409 keeps meaning "stale version" only |
| Demo users | A separate Flyway location, so they can be left out anywhere but a demo |

I also corrected the plan before approving it: it declared `password_hash varchar(72)`, mixing up two BCrypt numbers
(72 is the password byte limit; a hash is 60 characters). The column became `varchar(100)`.

## 3. The output

[task-api](task-api), built in eight slices: skeleton, login, create and read, list, update, status, delete, end to
end and Docker. Every slice was reviewed before the next.

```
com.example.tasks
├── controller      REST controllers, request/response records, RFC 9457 exception handler
├── service         one interface per use case, TaskService, LogInService (plain Java)
├── entity          Task, TaskStatus, TaskTitle, TaskDescription, User, Username (plain Java)
├── repository      TaskRepository, UserRepository, filter, sort and page values (plain Java)
│   └── jpa         row classes, Spring Data repositories, mapper, specifications
└── config          security (JWT, BCrypt), strict JSON, OpenAPI, wiring
```

A sample. The status rules live on the enum, and the task applies them:

```java
public Set<TaskStatus> nextStatuses() {
    return switch (this) {
        case TODO -> EnumSet.of(IN_PROGRESS, DONE);
        case IN_PROGRESS -> EnumSet.of(TODO, DONE);
        case DONE -> EnumSet.of(IN_PROGRESS);
    };
}
```

```java
/** Returns this same task when the status is already {@code target}, so a retried request changes nothing. */
public Task changeStatus(TaskStatus target, Instant now) {
    if (target == status) {
        return this;
    }
    if (!status.canMoveTo(target)) {
        throw new InvalidStatusChangeException(status, target);
    }
    return new Task(id, owner, title, description, target, dueDate, createdAt, now, version);
}
```

The test that came first, and failed before `changeStatus` existed:

```java
@Test
void aRefusedStatusChangeSaysFromWhereToWhereAndWhatIsAllowed() {
    var done = Task.create(ALICE, TaskTitle.of("Report"), null, null, NOW, TODAY).changeStatus(TaskStatus.DONE, NOW);

    assertThatExceptionOfType(InvalidStatusChangeException.class)
            .isThrownBy(() -> done.changeStatus(TaskStatus.TODO, NOW))
            .satisfies(refused -> {
                assertThat(refused.from()).isEqualTo(TaskStatus.DONE);
                assertThat(refused.to()).isEqualTo(TaskStatus.TODO);
                assertThat(refused.allowed()).containsExactly(TaskStatus.IN_PROGRESS);
            });
}
```

The service only orchestrates: ownership, the version check, then the entity's rule:

```java
public Task update(UserId caller, TaskId id, TaskChanges changes) {
    var stored = get(caller, id);
    if (stored.version() != changes.version()) {
        throw new TaskVersionConflictException(id, changes.version(), stored.version());
    }
    var now = now();
    var edited = stored.edit(TaskTitle.of(changes.title()), TaskDescription.of(changes.description()).orElse(null),
            changes.dueDate(), now, today(now));
    return tasks.update(edited);
}
```

### Endpoints

Every task endpoint needs `Authorization: Bearer <token>` and only sees the caller's own tasks.

| Method | Path | Success | Errors |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/login` | 200 `{accessToken, tokenType, expiresAt}` | 400, 401 |
| POST | `/api/v1/tasks` | 201 + `Location` | 400, 401 |
| GET | `/api/v1/tasks?status=&dueFrom=&dueTo=&sort=&page=&size=` | 200 page | 400, 401 |
| GET | `/api/v1/tasks/{id}` | 200 | 400, 401, 404 |
| PUT | `/api/v1/tasks/{id}` | 200 | 400, 401, 404, 409 |
| PATCH | `/api/v1/tasks/{id}/status` | 200 | 400, 401, 404, 409, 422 |
| DELETE | `/api/v1/tasks/{id}?version=` | 204 | 400, 401, 404, 409 |

```bash
TOKEN=$(curl -s -X POST localhost:8090/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"username": "alice", "password": "wonderland123"}' | jq -r .accessToken)

curl -X POST localhost:8090/api/v1/tasks -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title": "Buy milk", "description": "2 liters", "dueDate": "2030-01-31"}'

curl "localhost:8090/api/v1/tasks?status=TODO&sort=dueDate,asc" -H "Authorization: Bearer $TOKEN"

curl localhost:8090/api/v1/tasks/<id> -H "Authorization: Bearer $TOKEN"

# Send back the version you read; a stale one gets 409.
curl -X PUT localhost:8090/api/v1/tasks/<id> -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"version": 0, "title": "Buy oat milk", "dueDate": "2030-01-31"}'

curl -X PATCH localhost:8090/api/v1/tasks/<id>/status -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"version": 1, "status": "IN_PROGRESS"}'

curl -X DELETE "localhost:8090/api/v1/tasks/<id>?version=2" -H "Authorization: Bearer $TOKEN"
```

### Database

The schema is owned by Flyway; Hibernate only validates it.

| Object | Why |
| --- | --- |
| `users` | The existing User model: id, unique lowercase username, BCrypt hash |
| `tasks` | Owner, title, description, status, due date, `timestamptz` timestamps, `version` for optimistic locking |
| `fk_tasks_owner` | Every task has an owner that exists; no cascade, so a user with tasks cannot be deleted by accident |
| `ck_tasks_status`, `ck_tasks_title`, `ck_tasks_version` | The domain rules again, as a last line of defence against a bug or a manual edit |
| `ix_tasks_owner_status`, `ix_tasks_owner_due_date` | Every list query filters by owner first, then by status or due date |
| `R__demo_users` | Demo accounts in a separate location; repeatable, so later schema versions can still be added |

## 4. Design patterns, clean code and TDD

### Design patterns

| Pattern | Where | Why |
| --- | --- | --- |
| Aggregate | `Task` | All rules about a task (due date, status changes, ownership) live on it |
| Value Object | `TaskTitle`, `TaskDescription`, `Username`, `TaskFilter`, `TaskSort` | An invalid value cannot exist |
| Static Factory | `Task.create`, `TaskTitle.of`, `TaskSort.parse` | Names the intent and normalizes input before checking it |
| State (lightweight) | `TaskStatus.nextStatuses()` | The transition table lives in one place |
| Repository, Ports and Adapters | `TaskRepository`, `UserRepository`, `PasswordHasher`, `TokenIssuer` and their implementations | Services never see JPA, BCrypt or JWT; tests use fakes |
| Specification | `TaskSpecifications` | The list query built from optional filters |
| DTO and Mapper | `CreateTaskRequest`, `TaskResponse`; `TaskMapper` | Neither the API nor the table shape leaks into the entity |
| Optimistic Locking | version check in `TaskService`, `@Version`, `DELETE ... WHERE version = ?` | No lost updates, including the race between check and write |
| Template Method | `ApiExceptionHandler` overrides Spring's handler hooks | Spring's own errors get the same problem shape |
| Interface Segregation | `CreateTaskUseCase`, `UpdateTaskUseCase`, ... | Each controller method depends only on what it uses |

### Clean code

- **Rules in one place.** Field rules live in the value objects and the aggregate; request records carry no
  validation of their own, and the database repeats the rules only as a safety net.
- **Immutable entities.** `Task` is a record; every change returns a new task, so nothing is half-updated.
- **Names that state intent:** `findByIdAndOwner`, `nextStatuses`, `requireExistingAccount`; tests read as
  sentences (`anotherUsersTaskLooksExactlyLikeAMissingOne`).
- **No hidden time.** A `Clock` is injected; nothing calls `now()` in entities or services, so date rules are tested
  at fixed moments (for example 22:30 UTC with a clock in UTC+9).
- **Comments say why**, never what; no dead code, no unused parameters, constructor injection only.
- **Boundaries checked by the build.** ArchUnit fails if services, entities or repository interfaces import a
  framework, if anything depends on `controller`, `config` or `repository.jpa`, or if a controller touches a
  repository.

### TDD

Every behaviour started as a test that was run and seen failing for the expected reason, then the code, then a
refactor. A test that failed for the wrong reason was fixed before any code was written. The end-to-end tests,
written after the parts they combine, passed on their first run, as they should.

| Level | Tests | How |
| --- | --- | --- |
| Entity | 37 | Plain unit tests: value objects, the task's rules, the transition table |
| Service | 22 | In-memory fake repository and a fixed `Clock`; no mocks of our own code |
| Repository | 28 | Real PostgreSQL (Testcontainers) with the real migrations, including the constraints |
| Controller | 46 | Web slices: status codes, the error shape, strict JSON, every invalid parameter |
| Config | 5 | Token issuing and checking |
| End to end and architecture | 13 | Two users logging in over HTTP and running the full lifecycle; ArchUnit rules; startup and migrations |

## 5. How I validated the AI's suggestions

| Check | What it caught |
| --- | --- |
| Reviewing the plan before any code | The `password_hash` size |
| Seeing every test fail for the right reason first | A JWT expiry reported with microseconds while the token had whole seconds |
| Architecture rules as tests | Filter and sort types placed in `service`, which would have made repositories depend on services |
| Reviewing each slice before the next | The package structure; `PUT` clearing omitted fields, kept on purpose and documented |
| Running the Docker stack and sending hostile requests (tampered, expired and `alg: none` tokens, wrong types, control characters, huge payloads, stale versions, other users' ids) | Two 500s no test had exercised (below) |

The tool showed test output rather than "it works", and every slice ended with the whole suite passing.

## 6. Corrections and improvements

| What the AI produced | Why it was wrong | Correction | Caught by |
| --- | --- | --- | --- |
| `password_hash varchar(72)` | 72 is the password byte limit; a hash is 60 characters | `varchar(100)`, and a 400 for passwords over 72 bytes | My plan review |
| Demo users as migration `V100` | Flyway would then refuse every later `V2`, `V3`... as out of order | Repeatable `R__demo_users.sql` with `ON CONFLICT DO NOTHING` | The tool while implementing; I approved |
| Packages `domain`, `application`, `infrastructure`, `web` | Not the structure I want | `controller`, `service`, `entity`, `repository`, `config`, with the same ArchUnit guarantees | My review |
| Login `expiresAt` with microseconds | JWT times are whole seconds; the response promised more than the token allowed | Truncate to seconds | A failing test |
| Filter, sort and page types in `service` | The repository interface would depend on services | Moved next to the repository contract; new rule: controllers never use a repository | ArchUnit |
| A valid token for an account that no longer exists | Creating a task gave a 500 (foreign key violation) | The security chain checks the account exists: 401; deleting a user revokes their tokens | Hostile requests |
| A valid token whose subject is not a user id | 500 | A JWT validator requires a UUID subject: 401 | Hostile requests |
| `new JwtAuthenticationToken(jwt)` in that fix | That constructor builds an unauthenticated token: every request got 403 | The constructor that marks it authenticated | The end-to-end test |
| No rule about commits in the prompt | I commit every change myself | Added to the working agreement | My review |

## 7. Edge cases, authentication and validation

### Edge cases

| Case | Behaviour |
| --- | --- |
| Another user's task (read, list, update, delete) | 404, the same as a missing one; it is never even loaded |
| Two edits from the same read | The second gets 409 with both versions; nothing changes |
| Race between the version check and the write | `@Version` or the versioned `DELETE` refuses it: 409 |
| Same status again, deleting twice | 200 with no change; 404 |
| Due date in the past | Refused on create, and on update only when it changes |
| Unknown or internal sort field | 400 listing the accepted fields, never a 500 |
| Tasks without a due date, ties in the sort | Always last; creation time then id break ties, so paging never repeats or skips |
| `dueTo` before `dueFrom`, page size 0 or 51 | 400 naming the parameter |
| NUL or other control characters, oversized text | 400 naming the field |

### Authentication

| Case | Behaviour |
| --- | --- |
| No token | 401 problem with `WWW-Authenticate: Bearer`; anything not explicitly public needs a token |
| Tampered, `alg: none`, expired or foreign-issuer token | 401 |
| Valid token whose account no longer exists, or whose subject is not a user id | 401 |
| Wrong password vs unknown user | The same 401 and about the same response time (a dummy BCrypt check) |
| Password over 72 bytes | 400: BCrypt would ignore the rest |
| Secrets | Signing key of at least 32 bytes from `TASKS_SECURITY_JWT_SECRET`, or the API refuses to start |

### Validation

- Every error is an RFC 9457 problem (`application/problem+json`), including Spring's own 404, 405, 415 and 500.
  Invalid input lists each problem in `errors`, by `field` or `parameter`.
- JSON is read strictly: `"3"` is not a number, `1.7` is not truncated, unknown fields (including `id` or `status`
  on create) and duplicate fields are refused.
- Messages are in English whatever the server's locale.

```json
{
  "title": "Invalid request",
  "status": 400,
  "detail": "One or more fields are invalid.",
  "errors": [{ "field": "dueDate", "message": "must be today or later" }]
}
```

## Out of scope

Registration and user management (a User model is assumed to exist), refresh tokens, rate limiting on login, and
soft delete.
