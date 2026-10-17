import type { PokemonDetails } from './types'

const SPRITES = 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/'

export const bulbasaurDetails: PokemonDetails = {
  id: 1,
  name: 'bulbasaur',
  speciesName: 'bulbasaur',
  artworkUrl: `${SPRITES}other/official-artwork/1.png`,
  category: 'Seed Pokémon',
  description: 'While it is young, it uses the nutrients that are stored in the seed on its back in order to grow.',
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
  evolutionChain: {
    speciesId: 1,
    name: 'bulbasaur',
    spriteUrl: `${SPRITES}1.png`,
    condition: null,
    evolvesTo: [
      {
        speciesId: 2,
        name: 'ivysaur',
        spriteUrl: `${SPRITES}2.png`,
        condition: { trigger: 'level-up', minLevel: 16, item: null },
        evolvesTo: [
          {
            speciesId: 3,
            name: 'venusaur',
            spriteUrl: `${SPRITES}3.png`,
            condition: { trigger: 'level-up', minLevel: 32, item: null },
            evolvesTo: [],
          },
        ],
      },
    ],
  },
}

export const eeveeDetails: PokemonDetails = {
  ...bulbasaurDetails,
  id: 133,
  name: 'eevee',
  speciesName: 'eevee',
  category: 'Evolution Pokémon',
  types: ['normal'],
  evolutionChain: {
    speciesId: 133,
    name: 'eevee',
    spriteUrl: `${SPRITES}133.png`,
    condition: null,
    evolvesTo: [
      { speciesId: 134, name: 'vaporeon', spriteUrl: `${SPRITES}134.png`, condition: { trigger: 'use-item', minLevel: null, item: 'water-stone' }, evolvesTo: [] },
      { speciesId: 135, name: 'jolteon', spriteUrl: `${SPRITES}135.png`, condition: { trigger: 'use-item', minLevel: null, item: 'thunder-stone' }, evolvesTo: [] },
      { speciesId: 196, name: 'espeon', spriteUrl: `${SPRITES}196.png`, condition: { trigger: 'level-up', minLevel: null, item: null }, evolvesTo: [] },
    ],
  },
}

/** An alternate form: its own name differs from the species named in the evolution chain. */
export const alolanRaichuDetails: PokemonDetails = {
  ...bulbasaurDetails,
  id: 10100,
  name: 'raichu-alola',
  speciesName: 'raichu',
  types: ['electric', 'psychic'],
  evolutionChain: {
    speciesId: 172,
    name: 'pichu',
    spriteUrl: `${SPRITES}172.png`,
    condition: null,
    evolvesTo: [
      {
        speciesId: 25,
        name: 'pikachu',
        spriteUrl: `${SPRITES}25.png`,
        condition: { trigger: 'level-up', minLevel: null, item: null },
        evolvesTo: [
          { speciesId: 26, name: 'raichu', spriteUrl: `${SPRITES}26.png`, condition: { trigger: 'use-item', minLevel: null, item: 'thunder-stone' }, evolvesTo: [] },
        ],
      },
    ],
  },
}
