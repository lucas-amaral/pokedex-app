import { useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { ErrorState } from '../../components/ErrorState'
import { StatBars } from '../../components/StatBars'
import { TypeBadges } from '../../components/TypeBadges'
import { formatDate, formatDexNumber, formatHeight, formatPokemonName, formatWeight } from '../../lib/format'
import { useDocumentTitle } from '../../lib/useDocumentTitle'
import { collectionKeys, useCollectedPokemon, useSyncPokemon } from './collectionQueries'
import { EditFieldsForm } from './EditFieldsForm'
import { RemoveFromCollection } from './RemoveFromCollection'
import type { CollectedPokemon } from './types'

/** The service answers 400 for an id that is not a UUID and 404 for an unknown one; both mean "not here". */
const NOT_IN_COLLECTION = new Set([400, 404])

export function CollectedPokemonPage() {
  const { id = '' } = useParams()
  const { data, error, isPending, refetch } = useCollectedPokemon(id)
  const isMissing = error !== null && NOT_IN_COLLECTION.has(error.status)

  return (
    <div className="space-y-4">
      <Link to="/collection" className="inline-flex items-center gap-1 text-sm text-slate-500 hover:text-slate-900">
        <span aria-hidden="true">←</span> Back to collection
      </Link>

      {isPending && (
        <div role="status" className="h-96 animate-pulse rounded-xl border border-slate-200 bg-white">
          <span className="sr-only">Loading Pokémon…</span>
        </div>
      )}

      {isMissing && <NotInCollection />}
      {error && !isMissing && <ErrorState error={error} onRetry={() => void refetch()} />}
      {data && <Collected pokemon={data} />}
    </div>
  )
}

function NotInCollection() {
  useDocumentTitle('Not in your collection')

  return (
    <div className="rounded-xl border border-slate-200 bg-white p-6 text-center">
      <h1 className="text-lg font-semibold">Pokémon not in your collection</h1>
      <p className="mt-1 text-sm text-slate-500">It may have been removed, or the address is wrong.</p>
      <Link to="/collection" className="mt-4 inline-block text-sm font-medium text-red-600 hover:underline">
        Back to your collection
      </Link>
    </div>
  )
}

function Collected({ pokemon }: { pokemon: CollectedPokemon }) {
  const name = formatPokemonName(pokemon.name)
  const title = pokemon.nickname ?? name
  useDocumentTitle(title)
  const refresh = useSyncPokemon()

  return (
    <article aria-labelledby="collected-heading" className="grid gap-4 lg:grid-cols-[20rem_1fr]">
      <div className="space-y-4 rounded-xl border border-slate-200 bg-white p-4">
        {pokemon.artworkUrl && <img src={pokemon.artworkUrl} alt={name} width={256} height={256} className="mx-auto size-56" />}
        <div>
          <h1 id="collected-heading" className="text-2xl font-semibold">
            {title}
          </h1>
          <p className="text-sm text-slate-500">
            {name} · {formatDexNumber(pokemon.pokeapiId)}
          </p>
          <p className="text-sm text-slate-500">{pokemon.category ?? 'Unknown category'}</p>
        </div>
        <TypeBadges types={pokemon.types} />
        <dl className="grid grid-cols-2 gap-2 text-sm">
          <div className="rounded-lg bg-slate-50 p-2">
            <dt className="text-xs text-slate-500">Height</dt>
            <dd className="font-medium">{formatHeight(pokemon.heightM)}</dd>
          </div>
          <div className="rounded-lg bg-slate-50 p-2">
            <dt className="text-xs text-slate-500">Weight</dt>
            <dd className="font-medium">{formatWeight(pokemon.weightKg)}</dd>
          </div>
        </dl>
        <div className="space-y-2">
          <button
            type="button"
            onClick={() => refresh.mutate(String(pokemon.pokeapiId))}
            disabled={refresh.isPending}
            className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm font-medium hover:bg-slate-100 disabled:opacity-60"
          >
            {refresh.isPending ? 'Refreshing…' : 'Refresh from PokeAPI'}
          </button>
          {refresh.isSuccess && (
            <p role="status" className="text-sm text-green-700">
              PokeAPI data refreshed.
            </p>
          )}
          {refresh.isError && (
            <p role="alert" className="text-sm text-red-700">
              {refresh.error.title}
            </p>
          )}
          <p className="text-xs text-slate-400">Last synced {formatDate(pokemon.syncedAt)}</p>
          <Link to={`/pokemon/${pokemon.name}`} className="block text-sm font-medium text-red-600 hover:underline">
            View in catalog
          </Link>
          <RemoveFromCollection id={pokemon.id} title={title} />
        </div>
      </div>

      <div className="space-y-4">
        <YourFields pokemon={pokemon} />
        <section aria-labelledby="collected-stats-heading" className="rounded-xl border border-slate-200 bg-white p-4">
          <h2 id="collected-stats-heading" className="mb-3 font-semibold">
            Base stats
          </h2>
          <StatBars stats={pokemon.stats} />
        </section>
        {pokemon.description && (
          <p className="rounded-xl border border-slate-200 bg-white p-4 text-sm leading-relaxed text-slate-700">{pokemon.description}</p>
        )}
      </div>
    </article>
  )
}

/** The proprietary fields: data PokeAPI does not have and a sync never changes. Editable here (US04). */
function YourFields({ pokemon }: { pokemon: CollectedPokemon }) {
  const [isEditing, setEditing] = useState(false)
  const [justSaved, setJustSaved] = useState(false)
  const queryClient = useQueryClient()
  const languages = Object.entries(pokemon.localizedNames)
  const isEmpty = !pokemon.nickname && !pokemon.notes && !pokemon.region && languages.length === 0 && pokemon.tags.length === 0

  function startEditing() {
    setJustSaved(false)
    setEditing(true)
  }

  function reloadLatest() {
    setEditing(false)
    void queryClient.invalidateQueries({ queryKey: collectionKeys.item(pokemon.id) })
  }

  return (
    <section aria-labelledby="your-fields-heading" className="rounded-xl border border-slate-200 bg-white p-4">
      <div className="mb-3 flex items-center justify-between gap-2">
        <h2 id="your-fields-heading" className="font-semibold">
          Your fields
        </h2>
        {!isEditing && (
          <button type="button" onClick={startEditing} className="text-sm font-medium text-red-600 hover:underline">
            Edit your fields
          </button>
        )}
      </div>
      {justSaved && (
        <p role="status" className="mb-3 text-sm text-green-700">
          Saved.
        </p>
      )}
      {isEditing ? (
        <EditFieldsForm
          pokemon={pokemon}
          onSaved={() => {
            setEditing(false)
            setJustSaved(true)
          }}
          onCancel={() => setEditing(false)}
          onReloadLatest={reloadLatest}
        />
      ) : isEmpty ? (
        <p className="text-sm text-slate-500">No nickname, notes, region, names or tags yet.</p>
      ) : (
        <dl className="grid gap-3 text-sm sm:grid-cols-2">
          <Field label="Nickname" value={pokemon.nickname} />
          <Field label="Region" value={pokemon.region} />
          <div className="sm:col-span-2">
            <Field label="Notes" value={pokemon.notes} />
          </div>
          <div>
            <dt className="text-xs text-slate-500">Localized names</dt>
            <dd>
              {languages.length === 0 ? (
                <span className="text-slate-400">—</span>
              ) : (
                <ul className="space-y-0.5">
                  {languages.map(([language, localized]) => (
                    <li key={language}>
                      <span className="mr-2 rounded bg-slate-100 px-1.5 py-0.5 font-mono text-xs">{language}</span>
                      {localized}
                    </li>
                  ))}
                </ul>
              )}
            </dd>
          </div>
          <div>
            <dt className="text-xs text-slate-500">Tags</dt>
            <dd>
              {pokemon.tags.length === 0 ? (
                <span className="text-slate-400">—</span>
              ) : (
                <ul aria-label="Tags" className="flex flex-wrap gap-1">
                  {pokemon.tags.map((tag) => (
                    <li key={tag}>
                      <Link to={`/collection?tag=${encodeURIComponent(tag)}`} className="rounded-full border border-slate-300 px-2 py-0.5 text-xs hover:bg-slate-100">
                        #{tag}
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </dd>
          </div>
        </dl>
      )}
    </section>
  )
}

function Field({ label, value }: { label: string; value: string | null }) {
  return (
    <div>
      <dt className="text-xs text-slate-500">{label}</dt>
      <dd className="whitespace-pre-line">{value ?? <span className="text-slate-400">—</span>}</dd>
    </div>
  )
}
