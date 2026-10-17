import { formatDexNumber, formatPokemonName, formatWeight } from '../../lib/format'
import type { PokemonSummary } from './types'

interface PokemonCardProps {
  pokemon: PokemonSummary
}

export function PokemonCard({ pokemon }: PokemonCardProps) {
  const name = formatPokemonName(pokemon.name)
  const headingId = `pokemon-${pokemon.id}-name`

  return (
    <article
      aria-labelledby={headingId}
      className="flex h-full flex-col rounded-xl border border-slate-200 bg-white p-3 shadow-sm transition hover:border-slate-300 hover:shadow"
    >
      <div className="flex items-center gap-3">
        {pokemon.spriteUrl ? (
          <img
            src={pokemon.spriteUrl}
            alt={name}
            width={72}
            height={72}
            loading="lazy"
            className="size-18 shrink-0 [image-rendering:pixelated]"
          />
        ) : (
          <div aria-hidden="true" className="grid size-18 shrink-0 place-items-center rounded-lg bg-slate-100 text-2xl text-slate-300">
            ?
          </div>
        )}
        <div className="min-w-0">
          <p className="text-xs font-medium text-slate-400">{formatDexNumber(pokemon.id)}</p>
          <h2 id={headingId} className="truncate font-semibold">
            {name}
          </h2>
          <p className="truncate text-xs text-slate-500">{pokemon.category ?? 'Unknown category'}</p>
        </div>
      </div>
      <dl className="mt-3 flex items-center justify-between text-xs">
        <dt className="text-slate-500">Weight</dt>
        <dd className="font-medium">{formatWeight(pokemon.weightKg)}</dd>
      </dl>
      <ul aria-label="Abilities" className="mt-2 flex flex-wrap gap-1">
        {pokemon.abilities.map((ability) => (
          <li
            key={ability.name}
            title={ability.hidden ? 'Hidden ability' : undefined}
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
    </article>
  )
}
