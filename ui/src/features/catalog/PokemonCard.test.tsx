import { render, screen, within } from '@testing-library/react'
import type { ReactNode } from 'react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import { bulbasaur } from './fixtures'
import { PokemonCard } from './PokemonCard'

function inRouter({ children }: { children: ReactNode }) {
  return <MemoryRouter>{children}</MemoryRouter>
}

describe('PokemonCard', () => {
  it('links to the Pokémon details', () => {
    render(<PokemonCard pokemon={bulbasaur} />, { wrapper: inRouter })

    expect(screen.getByRole('link', { name: /Bulbasaur/ })).toHaveAttribute('href', '/pokemon/bulbasaur')
  })

  it('shows number, name, category and weight', () => {
    render(<PokemonCard pokemon={bulbasaur} />, { wrapper: inRouter })

    const card = screen.getByRole('article', { name: 'Bulbasaur' })
    expect(card).toHaveTextContent('#001')
    expect(card).toHaveTextContent('Seed Pokémon')
    expect(card).toHaveTextContent('6.9 kg')
  })

  it('shows the sprite with the Pokémon name as alternative text', () => {
    render(<PokemonCard pokemon={bulbasaur} />, { wrapper: inRouter })

    expect(screen.getByRole('img', { name: 'Bulbasaur' })).toHaveAttribute('src', bulbasaur.spriteUrl)
  })

  it('lists the abilities and marks the hidden one', () => {
    render(<PokemonCard pokemon={bulbasaur} />, { wrapper: inRouter })

    const abilities = within(screen.getByRole('list', { name: 'Abilities' })).getAllByRole('listitem')
    expect(abilities.map((item) => item.textContent)).toEqual(['Overgrow', 'Chlorophyll (hidden)'])
  })

  it('shows a placeholder when there is no sprite or category', () => {
    render(<PokemonCard pokemon={{ ...bulbasaur, spriteUrl: null, category: null }} />, { wrapper: inRouter })

    expect(screen.queryByRole('img', { name: 'Bulbasaur' })).not.toBeInTheDocument()
    expect(screen.getByRole('article', { name: 'Bulbasaur' })).toHaveTextContent('Unknown category')
  })
})
