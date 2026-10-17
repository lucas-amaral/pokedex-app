import { formatPokemonName } from '../lib/format'

/** Literal class names so Tailwind can find them when it scans the source. */
const TYPE_COLORS: Record<string, string> = {
  normal: 'bg-stone-200 text-stone-800',
  fire: 'bg-orange-100 text-orange-800',
  water: 'bg-sky-100 text-sky-800',
  electric: 'bg-yellow-100 text-yellow-800',
  grass: 'bg-green-100 text-green-800',
  ice: 'bg-cyan-100 text-cyan-800',
  fighting: 'bg-red-100 text-red-800',
  poison: 'bg-purple-100 text-purple-800',
  ground: 'bg-amber-100 text-amber-800',
  flying: 'bg-indigo-100 text-indigo-800',
  psychic: 'bg-pink-100 text-pink-800',
  bug: 'bg-lime-100 text-lime-800',
  rock: 'bg-yellow-200 text-yellow-900',
  ghost: 'bg-violet-100 text-violet-800',
  dragon: 'bg-indigo-200 text-indigo-900',
  dark: 'bg-neutral-300 text-neutral-900',
  steel: 'bg-slate-200 text-slate-800',
  fairy: 'bg-rose-100 text-rose-800',
}

export function TypeBadges({ types }: { types: string[] }) {
  return (
    <ul aria-label="Types" className="flex flex-wrap gap-1.5">
      {types.map((type) => (
        <li key={type} className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${TYPE_COLORS[type] ?? 'bg-slate-100 text-slate-700'}`}>
          {formatPokemonName(type)}
        </li>
      ))}
    </ul>
  )
}
