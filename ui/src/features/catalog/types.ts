/** Mirrors the service's PokemonSummaryResponse. */
export interface PokemonSummary {
  id: number
  name: string
  spriteUrl: string | null
  category: string | null
  weightKg: number
  abilities: Ability[]
}

export interface Ability {
  name: string
  hidden: boolean
}
