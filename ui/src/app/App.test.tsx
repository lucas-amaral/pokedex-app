import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { bulbasaur, catalogPage } from '../features/catalog/fixtures'
import { collectionPage } from '../features/collection/fixtures'
import { bulbasaurDetails } from '../features/pokemon-details/fixtures'
import { renderRoute } from '../test/renderRoute'
import { server } from '../test/server'

describe('App', () => {
  beforeEach(() => {
    server.use(
      http.get('*/api/v1/catalog/pokemon', () => HttpResponse.json(catalogPage([bulbasaur]))),
      http.get('*/api/v1/catalog/pokemon/:idOrName', () => HttpResponse.json(bulbasaurDetails)),
      http.get('*/api/v1/pokemon', () => HttpResponse.json(collectionPage([]))),
    )
  })

  it('shows the application name in the header', () => {
    renderRoute('/')

    expect(screen.getByRole('banner')).toHaveTextContent('Pokédex')
  })

  it('offers navigation to the collection', () => {
    renderRoute('/')

    expect(screen.getByRole('link', { name: 'Collection' })).toHaveAttribute('href', '/collection')
  })

  it('opens on the catalog', async () => {
    renderRoute('/')

    expect(await screen.findByRole('article', { name: 'Bulbasaur' })).toBeInTheDocument()
  })

  it('opens a Pokémon from the catalog and goes back to it', async () => {
    const user = userEvent.setup()
    renderRoute('/')

    await user.click(await screen.findByRole('link', { name: /Bulbasaur/ }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Bulbasaur' })).toBeInTheDocument()

    await user.click(screen.getByRole('link', { name: 'Catalog' }))
    expect(await screen.findByRole('article', { name: 'Bulbasaur' })).toBeInTheDocument()
  })

  it('returns to the same catalog page after opening a Pokémon', async () => {
    const user = userEvent.setup()
    const { router } = renderRoute('/?page=3')

    await user.click(await screen.findByRole('link', { name: /Bulbasaur/ }))
    await user.click(await screen.findByRole('link', { name: 'Back to catalog' }))

    expect(router.state.location.search).toBe('?page=3')
  })

  it('shows a not-found page for unknown addresses', () => {
    renderRoute('/no/such/page')

    expect(screen.getByRole('heading', { name: 'Page not found' })).toBeInTheDocument()
  })
})
