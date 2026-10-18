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

## Docker image

```bash
docker build -t pokedex-ui .
```

Node builds the static files; nginx serves them (`nginx.conf.template`). nginx does in production what the Vite proxy
does in development:

- forwards `/api/` to the service at `API_UPSTREAM` (default `http://service:8080`, the name in
  `../docker-compose.yml`), so the browser still talks to a single origin;
- answers any other unknown path with `index.html`, so addresses like `/collection/<id>` work on reload;
- caches `/assets/` for a year (file names contain a content hash) and revalidates `index.html` every time.

## Pages

| Address | Page |
| --- | --- |
| `/` and `/?page=N` | Catalog, page N (counted from 1, so the address can be shared or bookmarked) |
| `/pokemon/:idOrName` | One Pokémon: artwork, types, height, weight, abilities, base stats, description, evolution chain, and "Add to collection" (shown only when the Pokémon is not collected yet; otherwise "In your collection" with a link). A malformed address shows "Pokémon not found" without calling the service |
| `/collection?name=&tag=&page=N` | Your collection: filter by name, nickname or localized name and by tag (click a tag on a row); filters and page live in the address |
| `/collection/:id` | One collected Pokémon: PokeAPI data, your fields (nickname, notes, region, localized names, tags) with an edit form, "Refresh from PokeAPI", and "Remove from collection" with a confirmation |
| `/login` | Log in; afterwards returns to the page that sent you here. A logged-in user is sent on to that page |
| `/register` | Create an account; logs in right away and returns to the page that sent you here. A logged-in user is sent on to that page |
| anything else | Page not found |

## Logging in

Anyone can browse the catalog and the collection. What each visitor can change:

| Who | Can |
| --- | --- |
| Visitor | Browse; sees "Log in to add it to your collection" instead of the button |
| Logged-in user | Add a Pokémon, edit its fields, refresh it from PokeAPI |
| Admin (badge in the header) | Everything above, plus remove a Pokémon |

The service enforces the same rules; hiding the buttons only avoids offering actions that would fail.

- The token from `POST /api/v1/auth/login` is kept in `sessionStorage` and sent as `Authorization: Bearer …` on every
  call. It survives a reload and disappears when the tab closes. A stored token past its expiry is ignored.
- If the service answers 401 to a call that carried a token (expired or invalid), the app logs out and opens the login
  page with "Your session expired. Log in again."; after logging in you return to where you were.
- Why `sessionStorage`: the service issues stateless Bearer tokens, and this keeps the client simple. An `HttpOnly`
  cookie would hide the token from scripts (safer against XSS) but needs CSRF protection and a cookie-based service;
  `localStorage` would keep you logged in across tabs and restarts, but keeps the token around longer.

Routing uses React Router. In production the web server must answer every unknown path with `index.html` so these
addresses work on reload; the Docker image's nginx does.

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
├── main.tsx            Entry point: router and query client providers
├── app/                Layout (header, navigation), route table, not-found page, query client defaults
├── api/                HTTP helper, shared API types, ApiError
├── components/         Reusable UI pieces (Pagination, ErrorState, TypeBadges, StatBars)
├── features/
│   ├── auth/           Login and sign-up pages, AuthProvider and useAuth, session storage, auth API calls
│   ├── catalog/        Catalog screen: API call, useCatalogPage hook, PokemonCard, CatalogPage
│   ├── pokemon-details/ Detail screen: API call, usePokemonDetails hook, EvolutionChain
│   └── collection/     Collection: API calls, query keys and hooks, AddToCollectionButton, list and detail pages,
│                       EditFieldsForm (+ Zod schema), RemoveFromCollection
├── lib/                Formatting helpers and useDocumentTitle (names the browser tab after the page)
└── test/               Test setup: MSW server, query client wrapper, startLoggedInAs
```

Code is grouped by feature: everything a screen needs (API call, hook, components, tests) sits in one folder.
Shared pieces move to `components/`, `api/` or `lib/` only when a second feature needs them.

## State management

| Kind of state | Where it lives | Why |
| --- | --- | --- |
| Data from the API | TanStack Query queries | Caching per query, loading and error states, retries, previous page kept while the next one loads |
| Changes sent to the API (add, edit, refresh, remove) | TanStack Query mutations | The saved Pokémon goes straight into the cache and every collection list refetches, using shared query keys |
| Collection filters and page | The address (`?name=&tag=&page=`) | Same reasons as the catalog page |
| Edit, login and sign-up forms | React Hook Form, validated by Zod schemas | Fields, errors and submit without hand-written state; the schemas mirror the service's rules |
| Logged-in user | React context (`AuthProvider`, `useAuth`) backed by `sessionStorage` | Read by the header and by every gated button; the HTTP helper gets the token directly |
| Current catalog page | The address (`?page=N`) | Survives reloads, can be shared, works with the browser's back button |
| Where "Back to catalog" returns | React Router navigation state | Opening a Pokémon from page 7 and going back lands on page 7 again |

The query client retries server and network failures twice and never retries 4xx responses, which would fail again.

## Errors

Every failed call becomes an `ApiError` carrying the service's RFC 9457 problem details (`status`, `title`, `detail`,
`errors`), or a "Network error" when the service cannot be reached. Screens show the `title` and `detail` as sent by
the service and offer a retry.

## Editing

The edit form mirrors the service's rules in a Zod schema, so most mistakes are shown before anything is sent.
Errors the service still finds (`errors[]` in the problem details) are shown next to the matching field. The form
sends the `version` it was loaded with; if someone else saved in between, the service answers 409 and the form offers
"Load the latest version" instead of overwriting their change.

## Testing

98 tests in 13 files; run them with `npm test`.

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
| React Hook Form + Zod | Form state with little re-rendering; one schema for validation and for the TypeScript types |
| React Router | Addresses for each page, links, and the catalog page kept in the address |
| oxlint | The linter shipped with the Vite template; fast, with React hook rules |
