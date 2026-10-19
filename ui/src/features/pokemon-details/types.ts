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
  /** False for an alternate form (partner, regional, Mega...); its evolution chain is still the species'. */
  defaultForm: boolean
}

/** One stage of the evolution tree; the base form has no condition. */
export interface EvolutionNode {
  speciesId: number
  name: string
  spriteUrl: string | null
  condition: EvolutionCondition | null
  evolvesTo: EvolutionNode[]
}

/** Only the trigger is always there; the rest is null when that evolution does not need it. */
export interface EvolutionCondition {
  trigger: string
  minLevel: number | null
  item: string | null
  heldItem: string | null
  minHappiness: number | null
  timeOfDay: string | null
  knownMove: string | null
  knownMoveType: string | null
}
