# Thought process

How I approached the exercise: what I decided, in which order, and why. The [README](README.md) says what the
project is and how to run it; this document explains how it got there.

## 1. Plan before code

I started by mapping the brief to a list of requirements and open questions, with no code. Two rounds of
alignment settled the frame:

- **Stack:** Java 25 (current LTS) and Spring Boot 4.1. Boot 3.5's open-source support ended in June 2026, so a
  new project should not start on it.
- **Order:** backend first, story by story, with Swagger available from day one so every endpoint could be tried
  before any screen existed.
- **One early exception:** a thin UI slice (the catalog screen) built right after US01, time-boxed, to find
  front-end surprises early instead of on the last day. Then back to the backend.
- **Seed data:** Generations 1 and 2 (251 Pokémon), large enough to make search and paging meaningful.

Each user story then followed the same loop: a short plan (what changes, in which layer, which decisions are open),
small steps each closed with green tests, and a review at the end against the brief's acceptance criteria.

## 2. Architecture

The brief asks for business logic independent of the API and data access, so I used Clean Architecture with four
layers: `domain`, `application` (use cases and ports), `infrastructure` (PokeAPI and JPA adapters) and `web`.

- **Layers first, then features.** I compared this with a hexagonal, feature-first layout on this project's
  actual classes. With four stories, layers-first was easier to read.
- **Ports owned by the use cases.** PokeAPI and PostgreSQL are implementations behind interfaces, so use cases are
  tested with in-memory fakes and know nothing about HTTP or JPA.
- **Rules enforced, not just intended.** ArchUnit tests fail the build if an inner layer imports Spring, HTTP or
  persistence. I checked them by planting a violating class and watching them fail.
- **Small interfaces.** For US02, the detail lookup got its own port (`PokemonDetailsCatalog`) instead of a second
  method on the browse port, so the browse use case and its fake did not change (Interface Segregation).

## 3. Test-first, every step

Every step started with a failing test, run red for the expected reason, before the code that made it green. The
test level follows the risk:

| What | How it is tested |
| --- | --- |
| Domain and use cases | Plain unit tests with in-memory fakes |
| PokeAPI client | WireMock replaying real PokeAPI responses recorded as fixtures |
| Persistence | A real PostgreSQL 17 in Testcontainers, not an in-memory substitute |
| HTTP contract | `@WebMvcTest` for status codes, validation and error shapes |
| Whole flow | End-to-end tests over a real HTTP port |
| UI | Testing Library against a fake API (MSW), using the screens as a user would |

End-to-end tests usually passed on their first run. I expected that: they check that pieces already built
test-first fit together.

Coverage is a check, not a target. At the end I read the uncovered lines instead of chasing the percentage. That
found four real untested behaviours, for example a concurrent edit detected by the database. Each one got a test.

## 4. Decisions, story by story

### US01: browse

- **Declarative client** (`@HttpExchange` on `RestClient`): synchronous code, no reactive stack.
- **Virtual threads** to fetch the 20 Pokémon of a page in parallel. PokeAPI needs one call per Pokémon.
- **Caffeine cache** per PokeAPI resource with `@Cacheable(sync = true)`, so concurrent requests for the same
  Pokémon make one call. Failures are not cached. A repeated page dropped from 0.71 s to 0.004 s.
- **One error format:** RFC 9457 for every error. A PokeAPI outage is a 503 that does not leak internals.

The review found a real bug: `hasNext` overflowed at the last possible page. It also found that a custom executor
bean had silently switched off Spring Boot's own virtual-thread executor. Both were fixed with a test first.

### US02: details and evolution chain

- The evolution chain is a **Composite tree**, so a linear line (Bulbasaur) and an eight-way branch (Eevee) share one
  shape, in the API and in the recursive UI component.
- My manual testing later showed Leafeon with an outdated evolution method. PokeAPI now lists one method per game
  version, and the code took the first one. The test fixtures had been trimmed to the fields I used, so no test
  could catch it. I re-recorded the fixtures from live PokeAPI, wrote the failing tests, and switched to the entry
  PokeAPI marks as default.
- Then I audited all 540 evolution chains for similar cases. That added the Attack/Defense comparison for the
  Tyrogue family and "Remoraid in the party" for Mantine. Conditions that only matter from Generation 4 on are
  documented as not shown yet.

### US03: a local collection with proprietary fields

The key observation: a stored Pokémon has **two kinds of data with different owners**.

- **Source data** comes from PokeAPI: read-only, refreshed by a re-sync.
- **Proprietary fields** are ours: nickname, notes, region, localized names and tags.
- The central rule, written as a unit test first: **a re-sync refreshes the source data and never touches the
  proprietary fields.**

Other decisions:

- Normalized tables over JSONB, so names and tags are searchable and constrained.
- A UUID primary key plus a unique `pokeapi_id`.
- Flyway migrations for the schema.
- Evolution chains stay live from PokeAPI instead of being copied.
- Two simultaneous first syncs of the same Pokémon cannot create duplicates: the unique key rejects the second
  insert, and that request refreshes the row instead.

### US04: update with defensive logic

- `PUT` replaces all proprietary fields. A request that tries to change a source field (`name`, `weightKg`) is a 400
  naming that field, not silently ignored.
- **Optimistic locking:** the body carries the version that was read, and a stale one gets a 409 with the expected
  and current versions. The UI offers to load the latest version.
- To test the defensive logic I sent about 20 hostile payloads to the running API. Most were already handled.
  The ones that were not, each fixed test-first:
  - a NUL character gave a 500, because PostgreSQL cannot store it;
  - Jackson accepted `"3"` as a number and silently truncated a version of `1.7` to `1`;
  - a duplicated JSON field silently kept its last value.

  JSON is now read strictly, and every invalid value is a 400 that names the field.
- `DELETE` (admin only) completes the CRUD.

### Users and access

- **Stateless JWT** through Spring Security's OAuth2 resource server, so no token parsing is hand-written.
  Passwords are hashed with BCrypt.
- **Access rules:** reads are public, a login is needed to add or edit, and only `ADMIN` can remove.
  Registration always creates a `USER`; admins come only from configuration.
- **Login failures** look the same for an unknown user and a wrong password, timing included.
- **One shared collection.** The brief describes one local store plus users for authentication and protected
  routes. So users curate a shared collection; they do not own Pokémon. The UI wording says "the collection", not
  "your collection".
- **Token storage:** the UI keeps the token in `sessionStorage`. That is simpler than an HttpOnly cookie, which
  would need CSRF protection, and shorter-lived than `localStorage`.

### Seed and Docker

- **Seeding goes through the real sync use case,** with a different catalog implementation (Strategy) that reads a
  PokeAPI snapshot committed to the repository. Seeded rows obey the same rules as user-added ones, and startup
  needs no network.
- **Demo accounts come from configuration,** not a migration, so their public passwords can be switched off.
- **Docker:**
  - two-stage, non-root images;
  - an Alpine JRE, which brought the image from 623 MB to 435 MB;
  - nginx serves the UI and forwards `/api`, so there is no CORS in development or production;
  - `docker compose up --build` runs everything.

## 5. Mistakes and what they taught me

- **Trimmed fixtures hide bugs.** Fixtures cut down to the fields in use could not reveal a field I should have been
  using. Comparing against the live API, not only against the code, is what found it.
- **Zero is not "missing".** During the evolution audit, my own analysis script treated `0` as empty and missed
  Hitmontop, which is exactly the bug the code had to avoid. A test now covers the `0` case.
- **Flaky tests are bugs.** A PokeAPI client test failed one run in three. The JDK HTTP client attempted an HTTP/2
  upgrade that WireMock sometimes dropped. Pinning HTTP/1.1 fixed it, confirmed over repeated runs.
- **The database has its own precision.** PostgreSQL stores microseconds, Java produces nanoseconds, so a
  freshly-synced time did not match after a read. A test caught it and times are now truncated.

## 6. Known limits and next steps

Accepted on purpose; the token limits are documented in the [service README](service/README.md#access):

- A role change applies at the next login (no token revocation).
- No rate limit on login and register.
- An invalid token is refused even on public reads (RFC 6750 behaviour).
- The catalog lists every PokeAPI entry, alternate forms included, as PokeAPI does.

Natural next steps:

- **Per-user collections:** an owner column and queries scoped by owner.
- **Remaining evolution conditions:** gender, location, weather and similar, from Generation 4 on.
- **Rate limiting** and **refresh tokens**.

## 7. How I worked with AI

I used Claude Code as a pair programmer, never unchecked:

- I set the working rules up front: test-first, small steps, and nothing committed without my review.
- I approved a plan before any code.
- I reviewed every step and decided on every open question.
- I verified behaviour myself against the running application.

Several of the bugs above came from that verification, not from the tool.

The [GenAI exercise](genai/README.md) shows the same process on a separate task: the prompt, how it was refined
before coding, and how the output was validated and corrected.

## Result

- 318 service tests, 96% of lines covered (97 to 100% in the domain and use cases).
- 104 UI tests.
- One command (`docker compose up --build`) starts the database, the API and the UI with demo data.
