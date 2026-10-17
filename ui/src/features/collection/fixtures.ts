import type { Page } from '../../api/types'
import type { CollectedPokemon, CollectedPokemonSummary } from './types'

export const collectedBulbasaur: CollectedPokemon = {
  id: '6f1c2a3b-0000-4000-8000-000000000001',
  pokeapiId: 1,
  name: 'bulbasaur',
  speciesName: 'bulbasaur',
  category: 'Seed Pokémon',
  description: 'While it is young, it uses the nutrients that are stored in the seed on its back in order to grow.',
  spriteUrl: 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png',
  artworkUrl: 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/1.png',
  heightM: 0.7,
  weightKg: 6.9,
  types: ['grass', 'poison'],
  abilities: [
    { name: 'overgrow', hidden: false },
    { name: 'chlorophyll', hidden: true },
  ],
  stats: [
    { name: 'hp', value: 45 },
    { name: 'attack', value: 49 },
    { name: 'defense', value: 49 },
    { name: 'special-attack', value: 65 },
    { name: 'special-defense', value: 65 },
    { name: 'speed', value: 45 },
  ],
  nickname: 'Bulby',
  notes: 'Our first starter',
  region: 'Kanto',
  localizedNames: { ja: 'フシギダネ', 'pt-BR': 'Bulbassauro' },
  tags: ['starter'],
  syncedAt: '2026-10-18T10:00:00Z',
}

export const collectedCharmander: CollectedPokemon = {
  ...collectedBulbasaur,
  id: '6f1c2a3b-0000-4000-8000-000000000004',
  pokeapiId: 4,
  name: 'charmander',
  speciesName: 'charmander',
  category: 'Lizard Pokémon',
  types: ['fire'],
  nickname: null,
  notes: null,
  region: null,
  localizedNames: {},
  tags: [],
}

export function summaryOf(pokemon: CollectedPokemon): CollectedPokemonSummary {
  const { id, pokeapiId, name, nickname, spriteUrl, category, types, tags, syncedAt } = pokemon
  return { id, pokeapiId, name, nickname, spriteUrl, category, types, tags, syncedAt }
}

export function collectionPage(content: CollectedPokemon[], page = 0, size = 20, totalElements = content.length): Page<CollectedPokemonSummary> {
  const totalPages = Math.ceil(totalElements / size)
  return { content: content.map(summaryOf), page, size, totalElements, totalPages, hasNext: page < totalPages - 1 }
}
