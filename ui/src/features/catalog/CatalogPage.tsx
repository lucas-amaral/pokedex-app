import { useRef } from 'react'
import { useSearchParams } from 'react-router'
import { ErrorState } from '../../components/ErrorState'
import { Pagination } from '../../components/Pagination'
import { PokemonCard } from './PokemonCard'
import { useCatalogPage } from './useCatalogPage'

const PAGE_SIZE = 20

/** The address shows pages counted from 1 (?page=2); the API counts from 0. */
function pageFromAddress(value: string | null): number {
  const page = Number(value)
  return Number.isInteger(page) && page >= 1 ? page - 1 : 0
}

export function CatalogPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const page = pageFromAddress(searchParams.get('page'))
  const headingRef = useRef<HTMLHeadingElement>(null)
  const { data, error, isPending, isPlaceholderData, refetch } = useCatalogPage({ page, size: PAGE_SIZE })

  function goToPage(next: number) {
    setSearchParams({ page: String(next + 1) })
    headingRef.current?.scrollIntoView?.({ behavior: 'smooth', block: 'start' })
  }

  return (
    <section aria-labelledby="catalog-heading" className="space-y-4">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h1 id="catalog-heading" ref={headingRef} className="scroll-mt-4 text-xl font-semibold">
          Catalog
        </h1>
        {data && data.content.length > 0 && (
          <p className="text-sm text-slate-500">
            Showing {data.page * data.size + 1}–{data.page * data.size + data.content.length} of {data.totalElements}
          </p>
        )}
      </div>

      {isPending && <LoadingGrid />}

      {error && !data && <ErrorState error={error} onRetry={() => void refetch()} />}

      {data && (
        <>
          <ul
            aria-label="Pokémon"
            aria-busy={isPlaceholderData}
            className={`grid grid-cols-1 gap-3 transition-opacity sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-5 ${isPlaceholderData ? 'opacity-60' : ''}`}
          >
            {data.content.map((pokemon) => (
              <li key={pokemon.id}>
                <PokemonCard pokemon={pokemon} />
              </li>
            ))}
          </ul>
          <Pagination page={data.page} totalPages={data.totalPages} hasNext={data.hasNext} onPageChange={goToPage} />
        </>
      )}
    </section>
  )
}

function LoadingGrid() {
  return (
    <div role="status">
      <span className="sr-only">Loading Pokémon…</span>
      <div aria-hidden="true" className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-5">
        {Array.from({ length: PAGE_SIZE }, (_, index) => (
          <div key={index} className="h-40 animate-pulse rounded-xl border border-slate-200 bg-white" />
        ))}
      </div>
    </div>
  )
}
