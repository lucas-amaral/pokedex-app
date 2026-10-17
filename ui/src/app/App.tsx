import { Link, NavLink, Outlet, ScrollRestoration } from 'react-router'

/** Layout shared by every page: header with navigation, then the current route. */
export function App() {
  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex h-14 max-w-7xl items-center gap-6 px-4">
          <Link to="/" className="flex items-center gap-2 font-semibold">
            <img src="/favicon.svg" alt="" className="size-6" />
            Pokédex
          </Link>
          <nav aria-label="Main" className="flex gap-4 text-sm">
            <NavLink
              to="/"
              end
              className={({ isActive }) => (isActive ? 'font-medium text-red-600' : 'text-slate-600 hover:text-slate-900')}
            >
              Catalog
            </NavLink>
            <NavLink
              to="/collection"
              className={({ isActive }) => (isActive ? 'font-medium text-red-600' : 'text-slate-600 hover:text-slate-900')}
            >
              Your collection
            </NavLink>
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-7xl px-4 py-6">
        <Outlet />
      </main>
      <ScrollRestoration />
    </div>
  )
}
