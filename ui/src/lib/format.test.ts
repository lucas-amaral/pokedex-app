import { describe, expect, it } from 'vitest'
import { formatDexNumber, formatPokemonName, formatWeight } from './format'

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
