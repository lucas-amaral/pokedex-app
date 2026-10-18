import { Link, NavLink, Outlet, ScrollRestoration, useLocation } from 'react-router'
import { useAuth } from '../features/auth/authContext'
import { AuthProvider } from '../features/auth/AuthProvider'

/** Layout shared by every page: header with navigation and the logged-in user, then the current route. */
export function App() {
  return (
    <AuthProvider>
      <Layout />
    </AuthProvider>
  )
}

function Layout() {
  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex min-h-14 max-w-7xl flex-wrap items-center gap-x-6 gap-y-2 px-4 py-3">
          <Link to="/" className="flex items-center gap-2 font-semibold">
            <img src="/favicon.svg" alt="" className="size-6" />
            Pokédex
          </Link>
          <nav aria-label="Main" className="order-last flex w-full gap-4 text-sm whitespace-nowrap sm:order-none sm:w-auto">
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
          <Account />
        </div>
      </header>
      <main className="mx-auto max-w-7xl px-4 py-6">
        <Outlet />
      </main>
      <ScrollRestoration />
    </div>
  )
}

function Account() {
  const { user, isAdmin, signOut } = useAuth()
  const location = useLocation()

  if (!user) {
    return (
      <div className="ml-auto flex items-center gap-3 text-sm whitespace-nowrap">
        <Link to="/login" state={{ from: location }} className="font-medium text-slate-700 hover:text-slate-900">
          Log in
        </Link>
        <Link to="/register" state={{ from: location }} className="rounded-md bg-red-600 px-3 py-1.5 font-medium text-white hover:bg-red-700">
          Sign up
        </Link>
      </div>
    )
  }

  return (
    <div className="ml-auto flex items-center gap-3 text-sm whitespace-nowrap">
      <span className="text-slate-700">
        {user.username}
        {isAdmin && <span className="ml-1.5 rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800">Admin</span>}
      </span>
      <button type="button" onClick={signOut} className="text-slate-500 hover:text-slate-900">
        Log out
      </button>
    </div>
  )
}
