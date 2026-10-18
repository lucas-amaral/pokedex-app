import { type FormEvent, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { ErrorState } from '../../components/ErrorState'
import { Pagination } from '../../components/Pagination'
import { TypeBadges } from '../../components/TypeBadges'
import { formatDate, formatDexNumber, formatPokemonName } from '../../lib/format'
import { useDocumentTitle } from '../../lib/useDocumentTitle'
import { useCollectionPage } from './collectionQueries'
import type { CollectedPokemonSummary } from './types'

const PAGE_SIZE = 20

/** The address keeps the filters and the page (counted from 1): /collection?name=bulb&tag=starter&page=2. */
function readAddress(searchParams: URLSearchParams) {
  const page = Number(searchParams.get('page'))
  return {
    name: searchParams.get('name') ?? '',
    tag: searchParams.get('tag') ?? '',
    page: Number.isInteger(page) && page >= 1 ? page - 1 : 0,
  }
}

export function CollectionPage() {
  useDocumentTitle('Your collection')
  const [searchParams, setSearchParams] = useSearchParams()
  const { name, tag, page } = readAddress(searchParams)
  const { data, error, isPending, isPlaceholderData, refetch } = useCollectionPage({ page, size: PAGE_SIZE, name, tag })
  const isFiltered = name !== '' || tag !== ''

  function applyFilters(next: { name: string; tag: string }, nextPage = 0) {
    const params: Record<string, string> = {}
    if (next.name.trim()) params.name = next.name.trim()
    if (next.tag.trim()) params.tag = next.tag.trim().toLowerCase()
    if (nextPage > 0) params.page = String(nextPage + 1)
    setSearchParams(params)
  }

  return (
    <section aria-labelledby="collection-heading" className="space-y-4">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h1 id="collection-heading" className="text-xl font-semibold">
          Your collection
        </h1>
        {data && (
          <p className="text-sm text-slate-500">
            {data.totalElements} Pokémon
          </p>
        )}
      </div>

      <FilterForm key={searchParams.toString()} name={name} tag={tag} onApply={applyFilters} isFiltered={isFiltered} />

      {isPending && (
        <div role="status" className="h-64 animate-pulse rounded-xl border border-slate-200 bg-white">
          <span className="sr-only">Loading your collection…</span>
        </div>
      )}

      {error && !data && <ErrorState error={error} onRetry={() => void refetch()} />}

      {data && data.totalElements === 0 && !isFiltered && (
        <div className="rounded-xl border border-slate-200 bg-white p-6 text-center">
          <h2 className="font-semibold">Your collection is empty</h2>
          <p className="mt-1 text-sm text-slate-500">Open a Pokémon in the catalog and add it to start your collection.</p>
          <Link to="/" className="mt-4 inline-block text-sm font-medium text-red-600 hover:underline">
            Browse the catalog
          </Link>
        </div>
      )}

      {data && data.totalElements === 0 && isFiltered && (
        <p className="rounded-xl border border-slate-200 bg-white p-6 text-center text-sm text-slate-500">
          No Pokémon match these filters.
        </p>
      )}

      {data && data.content.length > 0 && (
        <>
          <ul
            aria-label="Collected Pokémon"
            aria-busy={isPlaceholderData}
            className={`divide-y divide-slate-100 rounded-xl border border-slate-200 bg-white transition-opacity ${isPlaceholderData ? 'opacity-60' : ''}`}
          >
            {data.content.map((pokemon) => (
              <CollectionRow key={pokemon.id} pokemon={pokemon} onTag={(picked) => applyFilters({ name, tag: picked })} />
            ))}
          </ul>
          <Pagination
            page={data.page}
            totalPages={data.totalPages}
            hasNext={data.hasNext}
            onPageChange={(next) => applyFilters({ name, tag }, next)}
          />
        </>
      )}
    </section>
  )
}

interface FilterFormProps {
  name: string
  tag: string
  isFiltered: boolean
  onApply: (filters: { name: string; tag: string }) => void
}

function FilterForm({ name, tag, isFiltered, onApply }: FilterFormProps) {
  const [draftName, setDraftName] = useState(name)
  const [draftTag, setDraftTag] = useState(tag)

  function submit(event: FormEvent) {
    event.preventDefault()
    onApply({ name: draftName, tag: draftTag })
  }

  const fieldClass = 'w-full rounded-md border border-slate-300 bg-white px-3 py-1.5 text-sm'

  return (
    <form role="search" onSubmit={submit} className="flex flex-wrap items-end gap-2">
      <label className="min-w-48 flex-1 text-xs text-slate-500">
        Name, nickname or localized name
        <input type="search" value={draftName} onChange={(event) => setDraftName(event.target.value)} className={fieldClass} />
      </label>
      <label className="w-40 text-xs text-slate-500">
        Tag
        <input type="text" value={draftTag} onChange={(event) => setDraftTag(event.target.value)} className={fieldClass} />
      </label>
      <button type="submit" className="rounded-md border border-slate-300 bg-white px-3 py-1.5 text-sm font-medium hover:bg-slate-100">
        Search
      </button>
      {isFiltered && (
        <button
          type="button"
          onClick={() => onApply({ name: '', tag: '' })}
          className="px-2 py-1.5 text-sm text-slate-500 hover:text-slate-900"
        >
          Clear filters
        </button>
      )}
    </form>
  )
}

function CollectionRow({ pokemon, onTag }: { pokemon: CollectedPokemonSummary; onTag: (tag: string) => void }) {
  return (
    <li className="flex flex-col gap-2 p-3 sm:flex-row sm:items-center">
      <Link to={`/collection/${pokemon.id}`} className="flex min-w-0 flex-1 items-center gap-3 rounded-md hover:bg-slate-50">
        {pokemon.spriteUrl ? (
          <img src={pokemon.spriteUrl} alt="" width={48} height={48} loading="lazy" className="size-12 shrink-0 [image-rendering:pixelated]" />
        ) : (
          <span aria-hidden="true" className="grid size-12 shrink-0 place-items-center rounded bg-slate-100 text-slate-300">
            ?
          </span>
        )}
        <span className="min-w-0">
          <span className="block text-xs text-slate-400">{formatDexNumber(pokemon.pokeapiId)}</span>
          <span className="block truncate font-medium">
            {formatPokemonName(pokemon.name)}
            {pokemon.nickname && <span className="font-normal text-slate-500"> “{pokemon.nickname}”</span>}
          </span>
          <span className="block truncate text-xs text-slate-500">{pokemon.category ?? 'Unknown category'}</span>
        </span>
      </Link>
      <div className="flex flex-wrap items-center gap-2 sm:justify-end">
        <TypeBadges types={pokemon.types} />
        {pokemon.tags.map((tag) => (
          <button
            key={tag}
            type="button"
            aria-label={`Show Pokémon tagged ${tag}`}
            onClick={() => onTag(tag)}
            className="rounded-full border border-slate-300 px-2 py-0.5 text-xs text-slate-600 hover:bg-slate-100"
          >
            #{tag}
          </button>
        ))}
        <span className="text-xs text-slate-400">Synced {formatDate(pokemon.syncedAt)}</span>
      </div>
    </li>
  )
}
