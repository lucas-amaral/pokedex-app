# pokedex-service

Spring Boot REST API for the Pokédex App. It serves the PokeAPI catalog, and (in upcoming stories) a local collection
and user management.

## Requirements

- JDK 25. If it is missing, the Gradle toolchain resolver downloads one.
- Network access to `https://pokeapi.co` at runtime. Tests do not need it: PokeAPI is replaced by WireMock.

## Run

```bash
./gradlew bootRun
```

The API listens on port 8080. To use another port:

```bash
./gradlew bootRun --args='--server.port=8081'
```

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI document: http://localhost:8080/v3/api-docs
- Health: http://localhost:8080/actuator/health

## Test

```bash
./gradlew build
```

`build` compiles, runs every test and writes the coverage report to `build/reports/jacoco/test/html/index.html`.
To run tests only: `./gradlew test`.

| Kind | Tooling | Example |
| --- | --- | --- |
| Domain and use case unit tests | JUnit 5, AssertJ, hand-written fakes | `BrowseCatalogServiceTest` |
| PokeAPI adapter tests | WireMock serving trimmed real PokeAPI responses (`src/test/resources/pokeapi`) | `PokeApiCatalogAdapterTest` |
| Web layer tests | `@WebMvcTest`, MockMvc, Mockito | `CatalogControllerTest` |
| Cache tests | Spring Boot test + WireMock request counts | `PokeApiCachingTest` |
| End-to-end tests | Real HTTP port, only PokeAPI faked | `CatalogApiEndToEndTest` |
| Architecture rules | ArchUnit | `ArchitectureTest` |

## API

All endpoints are under `/api/v1`. Pages are zero-based.

| Method | Path | Description | Responses |
| --- | --- | --- | --- |
| GET | `/catalog/pokemon?page=0&size=20` | One page of the PokeAPI catalog (US01). `size` is 1 to 50 | 200, 400, 503 |

Example:

```bash
curl 'http://localhost:8080/api/v1/catalog/pokemon?page=0&size=2'
```

```json
{
  "content": [
    {
      "id": 1,
      "name": "bulbasaur",
      "spriteUrl": "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png",
      "category": "Seed Pokémon",
      "weightKg": 6.9,
      "abilities": [
        { "name": "overgrow", "hidden": false },
        { "name": "chlorophyll", "hidden": true }
      ]
    }
  ],
  "page": 0,
  "size": 2,
  "totalElements": 1351,
  "totalPages": 676,
  "hasNext": true
}
```

### Errors

Every error is an [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) problem detail with content type
`application/problem+json`:

```json
{
  "title": "Invalid request",
  "status": 400,
  "detail": "One or more request parameters are invalid.",
  "instance": "/api/v1/catalog/pokemon",
  "errors": [{ "parameter": "size", "message": "must be less than or equal to 50" }]
}
```

| Status | When |
| --- | --- |
| 400 | Invalid parameters (`errors` lists each one) or wrong types |
| 404 | Unknown route |
| 503 | PokeAPI is down, too slow or returned an error |
| 500 | Unexpected error; details are logged, never returned |

## Configuration

Settings live in `src/main/resources/application.yml` and can be overridden with environment variables
(for example `POKEDEX_POKEAPI_READ_TIMEOUT=10s`).

| Property | Default | Meaning |
| --- | --- | --- |
| `pokedex.pokeapi.base-url` | `https://pokeapi.co/api/v2` | PokeAPI root |
| `pokedex.pokeapi.connect-timeout` | `2s` | Connection timeout per call |
| `pokedex.pokeapi.read-timeout` | `5s` | Response timeout per call |
| `spring.cache.caffeine.spec` | `maximumSize=2000,expireAfterWrite=24h,recordStats` | Limits for each PokeAPI cache |
| `spring.threads.virtual.enabled` | `true` | Virtual threads for requests and background work |

## Code structure

```
com.lucasamaral.pokedex
├── domain          Entities and value objects with their invariants (plain Java)
│   └── pokemon       PokemonSummary, PokemonDetails, Weight, Height, BaseStat, EvolutionNode, ...
├── application     Use cases and ports (plain Java)
│   ├── catalog       BrowseCatalog, GetPokemonDetails, PokemonCatalog and PokemonDetailsCatalog ports
│   └── pagination    Page, PageQuery
├── infrastructure  Adapters and Spring wiring
│   ├── pokeapi       Declarative client, JSON records, mapper, adapter
│   └── config        Use case beans, cache, OpenAPI
└── web             HTTP delivery
    ├── catalog       Controller and response DTOs
    ├── pagination    PageResponse
    └── error         RFC 9457 exception handler
```

Rules enforced by `ArchitectureTest`:

- `web` and `infrastructure` are not used by any other layer.
- `application` is used only by `infrastructure` and `web`; `domain` only by the layers above it.
- `domain` and `application` do not import Spring, Jakarta Persistence, Servlet or Jackson.

## PokeAPI integration

- The list endpoint returns only names, so each entry of a page needs two more calls (pokemon and species). They run
  in parallel on virtual threads.
- Every PokeAPI resource is cached separately (Caffeine, 24 h), so a Pokémon fetched for one page is reused by other
  pages and by the detail view. Parallel requests for the same resource wait for a single call; failures are not
  cached.
- PokeAPI's 404 means "no such Pokémon"; any other failure or timeout becomes `CatalogUnavailableException` (503).
