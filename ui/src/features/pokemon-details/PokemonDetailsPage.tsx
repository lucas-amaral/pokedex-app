import { Link, useParams } from 'react-router'
import { useCatalogReturnPath } from '../../app/catalogReturn'
import { ErrorState } from '../../components/ErrorState'
import { StatBars } from '../../components/StatBars'
import { TypeBadges } from '../../components/TypeBadges'
import { formatDexNumber, formatHeight, formatPokemonName, formatWeight } from '../../lib/format'
import { useDocumentTitle } from '../../lib/useDocumentTitle'
import { AddToCollectionButton } from '../collection/AddToCollectionButton'
import { EvolutionChain } from './EvolutionChain'
import { isValidIdOrName } from './idOrName'
import type { PokemonDetails } from './types'
import { usePokemonDetails } from './usePokemonDetails'

const NOT_FOUND = 404

export function PokemonDetailsPage() {
  const { idOrName = '' } = useParams()
  const isWellFormed = isValidIdOrName(idOrName)
  const { data, error, isLoading, refetch } = usePokemonDetails(idOrName, { enabled: isWellFormed })
  const catalogPath = useCatalogReturnPath()
  const isNotFound = !isWellFormed || error?.status === NOT_FOUND

  return (
    <div className="space-y-4">
      <Link to={catalogPath} className="inline-flex items-center gap-1 text-sm text-slate-500 hover:text-slate-900">
        <span aria-hidden="true">←</span> Back to catalog
      </Link>

      {isLoading && (
        <div role="status" className="h-96 animate-pulse rounded-xl border border-slate-200 bg-white">
          <span className="sr-only">Loading Pokémon…</span>
        </div>
      )}

      {isNotFound && <PokemonNotFound detail={error?.detail} />}

      {error && !isNotFound && <ErrorState error={error} onRetry={() => void refetch()} />}

      {data && <Details pokemon={data} />}
    </div>
  )
}

function PokemonNotFound({ detail }: { detail?: string }) {
  useDocumentTitle('Pokémon not found')

  return (
    <div className="rounded-xl border border-slate-200 bg-white p-6 text-center">
      <h1 className="text-lg font-semibold">Pokémon not found</h1>
      <p className="mt-1 text-sm text-slate-500">{detail ?? 'There is no Pokémon with that number or name.'}</p>
      <Link to="/" className="mt-4 inline-block text-sm font-medium text-red-600 hover:underline">
        Back to the catalog
      </Link>
    </div>
  )
}

function Details({ pokemon }: { pokemon: PokemonDetails }) {
  const name = formatPokemonName(pokemon.name)
  useDocumentTitle(name)

  return (
    <article aria-labelledby="pokemon-heading" className="grid gap-4 lg:grid-cols-[20rem_1fr]">
      <div className="space-y-4 rounded-xl border border-slate-200 bg-white p-4">
        {pokemon.artworkUrl ? (
          <img src={pokemon.artworkUrl} alt={name} width={256} height={256} className="mx-auto size-56 sm:size-64" />
        ) : (
          <div aria-hidden="true" className="mx-auto grid size-56 place-items-center rounded-lg bg-slate-100 text-5xl text-slate-300">
            ?
          </div>
        )}
        <div>
          <p className="text-sm font-medium text-slate-400">{formatDexNumber(pokemon.id)}</p>
          <h1 id="pokemon-heading" className="text-2xl font-semibold">
            {name}
          </h1>
          <p className="text-sm text-slate-500">{pokemon.category ?? 'Unknown category'}</p>
        </div>
        <TypeBadges types={pokemon.types} />
        <AddToCollectionButton key={pokemon.id} idOrName={pokemon.name} pokeapiId={pokemon.id} />
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
        <div>
          <h2 className="mb-1.5 text-sm font-semibold">Abilities</h2>
          <ul className="flex flex-wrap gap-1">
            {pokemon.abilities.map((ability) => (
              <li
                key={ability.name}
                className={
                  ability.hidden
                    ? 'rounded-full border border-dashed border-slate-300 px-2 py-0.5 text-xs text-slate-500'
                    : 'rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-700'
                }
              >
                {formatPokemonName(ability.name)}
                {ability.hidden && <span className="text-slate-400"> (hidden)</span>}
              </li>
            ))}
          </ul>
        </div>
      </div>

      <div className="space-y-4">
        {pokemon.description && (
          <p className="rounded-xl border border-slate-200 bg-white p-4 text-sm leading-relaxed text-slate-700">
            {pokemon.description}
          </p>
        )}
        <section aria-labelledby="stats-heading" className="rounded-xl border border-slate-200 bg-white p-4">
          <h2 id="stats-heading" className="mb-3 font-semibold">
            Base stats
          </h2>
          <StatBars stats={pokemon.stats} />
        </section>
        <div className="rounded-xl border border-slate-200 bg-white p-4">
          <EvolutionChain chain={pokemon.evolutionChain} currentSpecies={pokemon.speciesName} />
        </div>
      </div>
    </article>
  )
}
