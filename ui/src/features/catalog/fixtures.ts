import type { Page } from '../../api/types'
import type { PokemonSummary } from './types'

export const bulbasaur: PokemonSummary = {
  id: 1,
  name: 'bulbasaur',
  spriteUrl: 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png',
  category: 'Seed Pokémon',
  weightKg: 6.9,
  abilities: [
    { name: 'overgrow', hidden: false },
    { name: 'chlorophyll', hidden: true },
  ],
}

export const ivysaur: PokemonSummary = {
  ...bulbasaur,
  id: 2,
  name: 'ivysaur',
  spriteUrl: 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/2.png',
  weightKg: 13,
}

export function catalogPage(content: PokemonSummary[], page = 0, size = 20, totalElements = 1351): Page<PokemonSummary> {
  const totalPages = Math.ceil(totalElements / size)
  return { content, page, size, totalElements, totalPages, hasNext: page < totalPages - 1 }
}
