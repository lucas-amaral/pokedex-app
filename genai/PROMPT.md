# Prompt

Sent to Claude Code, in an empty `task-api/` folder.

```markdown
Build a task management REST API. Other developers will read the code, run it and probe it, so correctness, a clean
structure and tests written first matter more than feature count. Work in small steps and show me evidence (test
output), not claims.

## Stack
Java 25, Spring Boot 4.1, Gradle (Kotlin DSL). PostgreSQL 17 with Flyway; Hibernate only validates the schema.
Spring Security with JWT. Tests: JUnit 5, AssertJ, Mockito, Testcontainers, ArchUnit. No Lombok.

## Domain
- A basic User model already exists: id, username, password hash. Seed two demo users and add
  `POST /api/v1/auth/login`, which returns a Bearer token. Every task endpoint requires one.
- A task has an id, an owner (a user), a title (required, at most 100 characters), a description (optional, at most
  2,000), a status (TODO, IN_PROGRESS, DONE; starts as TODO), a due date (optional) and created/updated timestamps.
- A user only sees and changes their own tasks. Another user's task must look exactly like a missing one (404).
- Every change is based on the version the client read; a stale version gets 409.
- Not decided yet, ask me before planning around them: which status changes are allowed, whether a past due date
  is allowed, what update looks like (full or partial), and how delete works.

## API and errors
CRUD under `/api/v1/tasks`, plus listing with pagination, a status filter and a due date range. Standard verbs and
status codes. Every error is an RFC 9457 problem, with an `errors` list naming each invalid field or parameter.
JSON is read strictly: wrong types and unknown fields are 400s, never silently coerced or ignored.

## Structure
Packages `controller`, `service`, `entity`, `repository` (JPA implementation in `repository/jpa`) and `config`.
Services, entities and repository interfaces are plain Java with no framework imports, and dependencies point
inward. Enforce both rules with ArchUnit, so the build fails when they break.

## Design patterns
Use a pattern only where it earns its place, and tell me which one you used, where and why. I expect at least:
- Value Objects for title and description, so an invalid value cannot exist;
- the task as an Aggregate that owns its rules (status changes, due date);
- Repository with ports and adapters, so services never see JPA;
- Specification for the list filters;
- DTOs and a mapper, so neither the API nor the table shape leaks into the entity.

## Clean code
Intention-revealing names, small methods, constructor injection, immutable objects. Comments explain why, never
what. No dead code, no unused parameters. Inject a `Clock`; never call `now()` in entities or services.

## TDD
For every behaviour: write the test, run it, show me it fails for the expected reason, then write the code, then
refactor. Entities and services: unit tests with in-memory fakes, not mocks of our own code. Controllers: web slice
tests for status codes and the error shape. Repositories: Testcontainers with the real migrations. One end-to-end
test over HTTP with a real token. Cover the edge cases explicitly: another user's task, a stale version, a refused
status change, wrong JSON types, unknown fields, page size limits, a missing or invalid token.

## How we work
1. First, no code. Send me your questions (each with a recommendation), your assumptions, and a plan: tables with
   constraints and indexes, endpoints, packages, and the order of the slices with the tests you will write first in
   each. Then wait for my answers.
2. Then one slice at a time. Stop after each one with the test output and the patterns you used, and wait for my
   review.
3. Never say something works without running it.
4. Never commit or push. I review and commit every change; suggest a commit message at the end of each slice.

## Done means
`./gradlew build` passes, `docker compose up` runs the API with its database, and the README explains how to log
in and call each endpoint, the decisions we made, and the design patterns used.
```
