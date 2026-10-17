import { CatalogPage } from '../features/catalog/CatalogPage'

export function App() {
  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex h-14 max-w-7xl items-center gap-6 px-4">
          <a href="/" className="flex items-center gap-2 font-semibold">
            <img src="/favicon.svg" alt="" className="size-6" />
            Pokédex
          </a>
          <nav aria-label="Main" className="flex gap-4 text-sm">
            <a href="/" aria-current="page" className="font-medium text-red-600">
              Catalog
            </a>
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-7xl px-4 py-6">
        <CatalogPage />
      </main>
    </div>
  )
}
