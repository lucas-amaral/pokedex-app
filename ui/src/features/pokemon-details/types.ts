import type { Stat } from '../../components/StatBars'
import type { Ability } from '../catalog/types'

export type { Stat }

/** Mirrors the service's PokemonDetailsResponse. */
export interface PokemonDetails {
  id: number
  /** PokeAPI name, which can be an alternate form ("raichu-alola"). */
  name: string
  /** The species, as named in the evolution chain ("raichu"). */
  speciesName: string
  artworkUrl: string | null
  category: string | null
  description: string | null
  heightM: number
  weightKg: number
  types: string[]
  abilities: Ability[]
  stats: Stat[]
  evolutionChain: EvolutionNode
}

/** One stage of the evolution tree; the base form has no condition. */
export interface EvolutionNode {
  speciesId: number
  name: string
  spriteUrl: string | null
  condition: EvolutionCondition | null
  evolvesTo: EvolutionNode[]
}

export interface EvolutionCondition {
  trigger: string
  minLevel: number | null
  item: string | null
}
