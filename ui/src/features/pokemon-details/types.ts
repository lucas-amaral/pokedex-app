import type { Ability } from '../catalog/types'

/** Mirrors the service's PokemonDetailsResponse. */
export interface PokemonDetails {
  id: number
  name: string
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

export interface Stat {
  name: string
  value: number
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
