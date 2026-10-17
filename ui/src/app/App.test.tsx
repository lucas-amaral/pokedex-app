import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { App } from './App'

describe('App', () => {
  it('shows the application name in the header', () => {
    render(<App />)

    expect(screen.getByRole('banner')).toHaveTextContent('Pokédex')
  })

  it('offers navigation to the catalog', () => {
    render(<App />)

    expect(screen.getByRole('navigation')).toHaveTextContent('Catalog')
  })
})
