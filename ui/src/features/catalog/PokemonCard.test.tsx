import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { bulbasaur } from './fixtures'
import { PokemonCard } from './PokemonCard'

describe('PokemonCard', () => {
  it('shows number, name, category and weight', () => {
    render(<PokemonCard pokemon={bulbasaur} />)

    const card = screen.getByRole('article', { name: 'Bulbasaur' })
    expect(card).toHaveTextContent('#001')
    expect(card).toHaveTextContent('Seed Pokémon')
    expect(card).toHaveTextContent('6.9 kg')
  })

  it('shows the sprite with the Pokémon name as alternative text', () => {
    render(<PokemonCard pokemon={bulbasaur} />)

    expect(screen.getByRole('img', { name: 'Bulbasaur' })).toHaveAttribute('src', bulbasaur.spriteUrl)
  })

  it('lists the abilities and marks the hidden one', () => {
    render(<PokemonCard pokemon={bulbasaur} />)

    const abilities = within(screen.getByRole('list', { name: 'Abilities' })).getAllByRole('listitem')
    expect(abilities.map((item) => item.textContent)).toEqual(['Overgrow', 'Chlorophyll (hidden)'])
  })

  it('shows a placeholder when there is no sprite or category', () => {
    render(<PokemonCard pokemon={{ ...bulbasaur, spriteUrl: null, category: null }} />)

    expect(screen.queryByRole('img', { name: 'Bulbasaur' })).not.toBeInTheDocument()
    expect(screen.getByRole('article', { name: 'Bulbasaur' })).toHaveTextContent('Unknown category')
  })
})
