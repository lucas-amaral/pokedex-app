import { describe, expect, it } from 'vitest'
import { formatDexNumber, formatEvolutionCondition, formatHeight, formatPokemonName, formatStatName, formatWeight } from './format'

describe('formatPokemonName', () => {
  it('capitalizes each word of a PokeAPI name', () => {
    expect(formatPokemonName('bulbasaur')).toBe('Bulbasaur')
    expect(formatPokemonName('mr-mime')).toBe('Mr Mime')
    expect(formatPokemonName('golisopod-mega')).toBe('Golisopod Mega')
  })
})

describe('formatDexNumber', () => {
  it('pads to three digits like the games do', () => {
    expect(formatDexNumber(1)).toBe('#001')
    expect(formatDexNumber(25)).toBe('#025')
    expect(formatDexNumber(1025)).toBe('#1025')
  })
})

describe('formatWeight', () => {
  it('shows kilograms with at most one decimal', () => {
    expect(formatWeight(6.9)).toBe('6.9 kg')
    expect(formatWeight(13)).toBe('13 kg')
    expect(formatWeight(999.9)).toBe('999.9 kg')
  })
})

describe('formatHeight', () => {
  it('shows meters with at most one decimal', () => {
    expect(formatHeight(0.7)).toBe('0.7 m')
    expect(formatHeight(2)).toBe('2 m')
  })
})

describe('formatStatName', () => {
  it('uses the short labels of the games', () => {
    expect(formatStatName('hp')).toBe('HP')
    expect(formatStatName('special-attack')).toBe('Sp. Atk')
    expect(formatStatName('special-defense')).toBe('Sp. Def')
    expect(formatStatName('speed')).toBe('Speed')
  })
})

describe('formatEvolutionCondition', () => {
  it('describes how a Pokémon evolves', () => {
    expect(formatEvolutionCondition({ trigger: 'level-up', minLevel: 16, item: null })).toBe('Lv. 16')
    expect(formatEvolutionCondition({ trigger: 'use-item', minLevel: null, item: 'water-stone' })).toBe('Water Stone')
    expect(formatEvolutionCondition({ trigger: 'level-up', minLevel: null, item: null })).toBe('Level Up')
    expect(formatEvolutionCondition({ trigger: 'trade', minLevel: null, item: null })).toBe('Trade')
  })
})
