import { render, screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { bulbasaur, catalogPage } from '../features/catalog/fixtures'
import { server } from '../test/server'
import { createQueryWrapper } from '../test/queryClient'
import { App } from './App'

describe('App', () => {
  beforeEach(() => {
    server.use(http.get('*/api/v1/catalog/pokemon', () => HttpResponse.json(catalogPage([bulbasaur]))))
  })

  it('shows the application name in the header', () => {
    render(<App />, { wrapper: createQueryWrapper() })

    expect(screen.getByRole('banner')).toHaveTextContent('Pokédex')
  })

  it('offers navigation to the catalog', () => {
    render(<App />, { wrapper: createQueryWrapper() })

    expect(screen.getByRole('navigation')).toHaveTextContent('Catalog')
  })

  it('opens on the catalog', async () => {
    render(<App />, { wrapper: createQueryWrapper() })

    expect(await screen.findByRole('article', { name: 'Bulbasaur' })).toBeInTheDocument()
  })
})
