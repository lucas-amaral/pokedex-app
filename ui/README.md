# pokedex-ui

React front-end for the Pokédex App. It talks only to the service's `/api` endpoints.

## Requirements

- Node.js 22.12 or newer
- The service running on http://localhost:8080 (see `../service/README.md`)

## Run

```bash
npm install
npm run dev
```

Open the URL Vite prints (http://localhost:5173 by default; Vite picks the next free port if it is taken).

The dev server forwards every `/api` request to the service, so the browser only ever talks to one origin and the
service needs no CORS setup. To point it at a service on another port:

```bash
API_PROXY_TARGET=http://localhost:8081 npm run dev
```

## Scripts

| Script | What it does |
| --- | --- |
| `npm run dev` | Dev server with hot reload and the `/api` proxy |
| `npm test` | Runs the test suite once |
| `npm run test:watch` | Re-runs tests on every save |
| `npm run typecheck` | TypeScript check of the whole project |
| `npm run lint` | oxlint (React and TypeScript rules) |
| `npm run build` | Type check, then a production build in `dist/` |
| `npm run preview` | Serves the production build locally |

## Code structure

```
src/
├── main.tsx            Entry point: React root and the query client provider
├── app/                Application shell (header, navigation) and query client defaults
├── api/                HTTP helper, shared API types, ApiError
├── components/         Reusable UI pieces (Pagination, ErrorState)
├── features/
│   └── catalog/        Catalog screen: API call, useCatalogPage hook, PokemonCard, CatalogPage
├── lib/                Formatting helpers (names, Pokédex numbers, weight)
└── test/               Test setup: MSW server, query client wrapper
```

Code is grouped by feature: everything a screen needs (API call, hook, components, tests) sits in one folder.
Shared pieces move to `components/`, `api/` or `lib/` only when a second feature needs them.

## State management

| Kind of state | Where it lives | Why |
| --- | --- | --- |
| Data from the API | TanStack Query | Caching per query, loading and error states, retries, previous page kept while the next one loads |
| Local UI state (current page) | `useState` in the screen | Only one screen needs it |

The query client retries server and network failures twice and never retries 4xx responses, which would fail again.

## Errors

Every failed call becomes an `ApiError` carrying the service's RFC 9457 problem details (`status`, `title`, `detail`,
`errors`), or a "Network error" when the service cannot be reached. Screens show the `title` and `detail` as sent by
the service and offer a retry.

## Testing

- **Vitest** runs the tests in a simulated browser (jsdom).
- **Testing Library** queries the page the way a user would: by role, label and visible text.
- **MSW** fakes the service's HTTP API, so tests exercise the real `fetch` calls. Any request a test did not expect
  fails the test.

## Tech choices

| Piece | Why |
| --- | --- |
| React 19 + TypeScript | Typed components; API types mirror the service DTOs |
| Vite 8 | Instant dev server, fast production build, dev proxy |
| Tailwind CSS 4 | Responsive layout with utility classes (1 column on phones, up to 5 on wide screens) |
| TanStack Query | Server state without hand-written loading, error and caching code |
| oxlint | The linter shipped with the Vite template; fast, with React hook rules |
