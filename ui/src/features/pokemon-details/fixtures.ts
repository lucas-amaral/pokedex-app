import type { EvolutionCondition, PokemonDetails } from './types'

/** A condition with only the fields a test cares about; the service sends the rest as null. */
export function evolutionCondition(fields: Partial<EvolutionCondition> & { trigger: string }): EvolutionCondition {
  return {
    minLevel: null,
    item: null,
    heldItem: null,
    minHappiness: null,
    timeOfDay: null,
    knownMove: null,
    knownMoveType: null,
    ...fields,
  }
}

const SPRITES = 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/'

export const bulbasaurDetails: PokemonDetails = {
  id: 1,
  name: 'bulbasaur',
  speciesName: 'bulbasaur',
  defaultForm: true,
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
        condition: evolutionCondition({ trigger: 'level-up', minLevel: 16 }),
        evolvesTo: [
          {
            speciesId: 3,
            name: 'venusaur',
            spriteUrl: `${SPRITES}3.png`,
            condition: evolutionCondition({ trigger: 'level-up', minLevel: 32 }),
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
      { speciesId: 134, name: 'vaporeon', spriteUrl: `${SPRITES}134.png`, condition: evolutionCondition({ trigger: 'use-item', item: 'water-stone' }), evolvesTo: [] },
      { speciesId: 135, name: 'jolteon', spriteUrl: `${SPRITES}135.png`, condition: evolutionCondition({ trigger: 'use-item', item: 'thunder-stone' }), evolvesTo: [] },
      { speciesId: 196, name: 'espeon', spriteUrl: `${SPRITES}196.png`, condition: evolutionCondition({ trigger: 'level-up', minHappiness: 160, timeOfDay: 'day' }), evolvesTo: [] },
    ],
  },
}

/** An alternate form: its own name differs from the species named in the evolution chain. */
export const alolanRaichuDetails: PokemonDetails = {
  ...bulbasaurDetails,
  id: 10100,
  name: 'raichu-alola',
  speciesName: 'raichu',
  defaultForm: false,
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
        condition: evolutionCondition({ trigger: 'level-up', minHappiness: 220 }),
        evolvesTo: [
          { speciesId: 26, name: 'raichu', spriteUrl: `${SPRITES}26.png`, condition: evolutionCondition({ trigger: 'use-item', item: 'thunder-stone' }), evolvesTo: [] },
        ],
      },
    ],
  },
}
