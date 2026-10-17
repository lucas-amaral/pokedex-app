# pokedex-service

Spring Boot REST API for the Pokédex App. It serves the PokeAPI catalog, and (in upcoming stories) a local collection
and user management.

## Requirements

- JDK 25. If it is missing, the Gradle toolchain resolver downloads one.
- PostgreSQL 17. From the repository root, `docker compose up -d db` starts one with the default credentials.
- A Docker runtime for the tests: Testcontainers starts its own PostgreSQL.
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
| Repository tests | `@DataJpaTest` on a real PostgreSQL started by Testcontainers, schema from Flyway | `JpaCollectionRepositoryTest` |
| End-to-end tests | Real HTTP port, only PokeAPI faked | `CatalogApiEndToEndTest` |
| Architecture rules | ArchUnit | `ArchitectureTest` |

## API

All endpoints are under `/api/v1`. Pages are zero-based.

| Method | Path | Description | Responses |
| --- | --- | --- | --- |
| GET | `/catalog/pokemon?page=0&size=20` | One page of the PokeAPI catalog (US01). `size` is 1 to 50 | 200, 400, 503 |
| POST | `/pokemon/sync` with `{"idOrName": "pikachu"}` | Copy a Pokémon from PokeAPI into the collection (US03): 201 with `Location` when added, 200 when it was already there (source data refreshed, proprietary fields kept) | 201, 200, 400, 404, 503 |
| GET | `/pokemon?page=0&size=20&name=pika&tag=starter` | The collection in Pokédex order; `name` matches part of the name or nickname, `tag` an exact tag | 200, 400 |
| GET | `/pokemon/{id}` | One collected Pokémon by its UUID: source data plus nickname, notes, region, localized names and tags | 200, 400, 404 |
| GET | `/catalog/pokemon/{idOrName}` | One Pokémon by Pokédex number (`25`) or name (`pikachu`, case-insensitive): artwork, base stats, description, evolution chain (US02) | 200, 400, 404, 503 |

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

`name` is the Pokémon itself and can be an alternate form (`raichu-alola`); `speciesName` is its species (`raichu`), as named in
the evolution chain. The detail view returns the evolution chain as a tree: each stage lists the species it can evolve into and the
condition (`trigger`, `minLevel`, `item`) to get there. Eevee is shown with 2 of its 8 branches.

```bash
curl 'http://localhost:8080/api/v1/catalog/pokemon/eevee'
```

```json
{
  "id": 133,
  "name": "eevee",
  "speciesName": "eevee",
  "spriteUrl": "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/133.png",
  "artworkUrl": "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/133.png",
  "category": "Evolution Pokémon",
  "description": "Harbors the potential to evolve into manifold forms. Within Eevee lies the key to the mysteries of Pokémon evolution—I'm certain of it.",
  "heightM": 0.3,
  "weightKg": 6.5,
  "types": [
    "normal"
  ],
  "abilities": [
    {
      "name": "run-away",
      "hidden": false
    },
    {
      "name": "adaptability",
      "hidden": false
    },
    {
      "name": "anticipation",
      "hidden": true
    }
  ],
  "stats": [
    {
      "name": "hp",
      "value": 55
    },
    {
      "name": "attack",
      "value": 55
    },
    {
      "name": "defense",
      "value": 50
    },
    {
      "name": "special-attack",
      "value": 45
    },
    {
      "name": "special-defense",
      "value": 65
    },
    {
      "name": "speed",
      "value": 55
    }
  ],
  "evolutionChain": {
    "speciesId": 133,
    "name": "eevee",
    "spriteUrl": "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/133.png",
    "condition": null,
    "evolvesTo": [
      {
        "speciesId": 134,
        "name": "vaporeon",
        "spriteUrl": "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/134.png",
        "condition": {
          "trigger": "use-item",
          "minLevel": null,
          "item": "water-stone"
        },
        "evolvesTo": []
      },
      {
        "speciesId": 135,
        "name": "jolteon",
        "spriteUrl": "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/135.png",
        "condition": {
          "trigger": "use-item",
          "minLevel": null,
          "item": "thunder-stone"
        },
        "evolvesTo": []
      }
    ]
  }
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
| 400 | Invalid parameters or body fields (`errors` lists each one, by `parameter` or `field`), wrong types, or a body that is not JSON |
| 404 | No Pokémon with that number or name ("Pokémon not found"), no such id in the collection ("Pokémon not in the collection"), or unknown route |
| 503 | PokeAPI is down, too slow or returned an error |
| 500 | Unexpected error; details are logged, never returned |

## Configuration

Settings live in `src/main/resources/application.yml` and can be overridden with environment variables
(for example `POKEDEX_POKEAPI_READ_TIMEOUT=10s`).

| Property | Default | Meaning |
| --- | --- | --- |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/pokedex` | Database (also `SPRING_DATASOURCE_URL`) |
| `spring.datasource.username` / `password` | `pokedex` / `pokedex` | Database credentials |
| `pokedex.pokeapi.base-url` | `https://pokeapi.co/api/v2` | PokeAPI root |
| `pokedex.pokeapi.connect-timeout` | `2s` | Connection timeout per call |
| `pokedex.pokeapi.read-timeout` | `5s` | Response timeout per call |
| `spring.cache.caffeine.spec` | `maximumSize=2000,expireAfterWrite=24h,recordStats` | Limits for each PokeAPI cache |
| `spring.threads.virtual.enabled` | `true` | Virtual threads for requests and background work |

## Code structure

```
com.lucasamaral.pokedex
├── domain          Entities and value objects with their invariants (plain Java)
│   ├── pokemon       PokemonSummary, PokemonDetails, Weight, Height, BaseStat, EvolutionNode, ...
│   └── collection    CollectedPokemon (aggregate), SourceData, CustomFields
├── application     Use cases and ports (plain Java)
│   ├── catalog       BrowseCatalog, GetPokemonDetails, PokemonCatalog and PokemonDetailsCatalog ports
│   ├── collection    SyncPokemon, BrowseCollection, CollectionRepository port, CollectionFilter
│   └── pagination    Page, PageQuery
├── infrastructure  Adapters and Spring wiring
│   ├── pokeapi       Declarative client, JSON records, mapper, adapter
│   ├── persistence   JPA entity, mapper, specifications, collection repository adapter
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

## Database

PostgreSQL, with the schema owned by Flyway (`src/main/resources/db/migration`); Hibernate only validates it.

| Table | Holds |
| --- | --- |
| `pokemon` | One collected Pokémon: UUID key, unique `pokeapi_id`, source data, proprietary text fields, timestamps, version |
| `pokemon_type`, `pokemon_ability`, `pokemon_stat` | Source data lists, in PokeAPI order |
| `pokemon_localized_name` | Proprietary names by language tag |
| `pokemon_tag` | Proprietary tags (indexed for filtering) |

A collected Pokémon has two owners. A sync refreshes the source data and never touches the proprietary fields
(nickname, notes, region, localized names, tags). The unique `pokeapi_id` stops two concurrent syncs from storing the
same Pokémon twice; the losing request refreshes the stored copy instead.

## PokeAPI integration

- The list endpoint returns only names, so each entry of a page needs two more calls (pokemon and species). They run
  in parallel on virtual threads.
- Every PokeAPI resource is cached separately (Caffeine, 24 h), so a Pokémon fetched for one page is reused by other
  pages and by the detail view. Parallel requests for the same resource wait for a single call; failures are not
  cached.
- PokeAPI's 404 means "no such Pokémon"; any other failure or timeout becomes `CatalogUnavailableException` (503).
