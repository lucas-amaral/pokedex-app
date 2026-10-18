import type { Stat } from '../../components/StatBars'
import type { Ability } from '../catalog/types'

/** Mirrors the service's CollectedPokemonSummaryResponse: one row of the collection. */
export interface CollectedPokemonSummary {
  id: string
  pokeapiId: number
  name: string
  nickname: string | null
  spriteUrl: string | null
  category: string | null
  types: string[]
  tags: string[]
  syncedAt: string
}

/** Mirrors the service's CollectedPokemonResponse: PokeAPI data, then the proprietary fields. */
export interface CollectedPokemon {
  id: string
  pokeapiId: number
  name: string
  speciesName: string
  category: string | null
  description: string | null
  spriteUrl: string | null
  artworkUrl: string | null
  heightM: number
  weightKg: number
  types: string[]
  abilities: Ability[]
  stats: Stat[]
  nickname: string | null
  notes: string | null
  region: string | null
  localizedNames: Record<string, string>
  tags: string[]
  syncedAt: string
  /** Sent back with an edit; the service refuses edits based on an older version (409). */
  version: number
}

export interface SyncResult {
  pokemon: CollectedPokemon
  /** true when the Pokémon was added; false when it was already there and only refreshed. */
  created: boolean
}
