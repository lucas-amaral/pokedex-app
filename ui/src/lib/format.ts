import type { EvolutionCondition } from '../features/pokemon-details/types'

/** "mr-mime" → "Mr Mime". PokeAPI names are lowercase and hyphenated. */
export function formatPokemonName(name: string): string {
  return name
    .split('-')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

/** 1 → "#001", 1025 → "#1025". */
export function formatDexNumber(id: number): string {
  return `#${String(id).padStart(3, '0')}`
}

const weightFormat = new Intl.NumberFormat('en', { maximumFractionDigits: 1 })

/** 6.9 → "6.9 kg", 13 → "13 kg". */
export function formatWeight(kilograms: number): string {
  return `${weightFormat.format(kilograms)} kg`
}

const heightFormat = new Intl.NumberFormat('en', { maximumFractionDigits: 1 })

/** 0.7 → "0.7 m". */
export function formatHeight(meters: number): string {
  return `${heightFormat.format(meters)} m`
}

const STAT_LABELS: Record<string, string> = {
  hp: 'HP',
  attack: 'Attack',
  defense: 'Defense',
  'special-attack': 'Sp. Atk',
  'special-defense': 'Sp. Def',
  speed: 'Speed',
}

/** "special-attack" → "Sp. Atk", the short labels used in the games. */
export function formatStatName(name: string): string {
  return STAT_LABELS[name] ?? formatPokemonName(name)
}

/** How the previous stage evolves: "Lv. 16", "Water Stone", "Trade", "Level Up". */
/**
 * How a stage is reached: the level, the item or the trigger, then whatever else it needs, so Espeon
 * ("Level Up · Friendship 160+ · Day") and Umbreon ("... · Night") read differently.
 */
export function formatEvolutionCondition(condition: EvolutionCondition): string {
  const parts = [mainRequirement(condition)]
  if (condition.heldItem !== null) parts.push(`Holding ${formatPokemonName(condition.heldItem)}`)
  if (condition.minHappiness !== null) parts.push(`Friendship ${condition.minHappiness}+`)
  if (condition.timeOfDay !== null) parts.push(formatPokemonName(condition.timeOfDay))
  if (condition.knownMove !== null) parts.push(`Knows ${formatPokemonName(condition.knownMove)}`)
  if (condition.knownMoveType !== null) parts.push(`Knows a ${formatPokemonName(condition.knownMoveType)} move`)
  return parts.join(' · ')
}

function mainRequirement(condition: EvolutionCondition): string {
  if (condition.minLevel !== null) return `Lv. ${condition.minLevel}`
  if (condition.item !== null) return formatPokemonName(condition.item)
  return formatPokemonName(condition.trigger)
}

const dateFormat = new Intl.DateTimeFormat('en', { dateStyle: 'medium' })

/** "2026-10-18T10:00:00Z" → "Oct 18, 2026". */
export function formatDate(isoInstant: string): string {
  return dateFormat.format(new Date(isoInstant))
}
