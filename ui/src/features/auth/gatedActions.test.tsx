import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { collectedBulbasaur, collectionPage } from '../collection/fixtures'
import { bulbasaurDetails } from '../pokemon-details/fixtures'
import { renderRoute } from '../../test/renderRoute'
import { server } from '../../test/server'
import { startLoggedInAs } from '../../test/session'
import { ash, oak } from './fixtures'

function serveBulbasaur() {
  server.use(
    http.get('*/api/v1/catalog/pokemon/:idOrName', () => HttpResponse.json(bulbasaurDetails)),
    http.get('*/api/v1/pokemon', () => HttpResponse.json(collectionPage([]))),
    http.get('*/api/v1/pokemon/:id', () => HttpResponse.json(collectedBulbasaur)),
  )
}

describe('actions shown by who is logged in', () => {
  it('asks a visitor to log in instead of offering to add', async () => {
    serveBulbasaur()
    renderRoute('/pokemon/bulbasaur')

    expect(await screen.findByRole('link', { name: 'Log in to add it to the collection' })).toHaveAttribute('href', '/login')
    expect(screen.queryByRole('button', { name: 'Add to collection' })).not.toBeInTheDocument()
  })

  it('lets a logged-in user add, sending the token', async () => {
    serveBulbasaur()
    const bearer = startLoggedInAs(ash)
    let authorization: string | null = null
    server.use(
      http.post('*/api/v1/pokemon/sync', ({ request }) => {
        authorization = request.headers.get('Authorization')
        return HttpResponse.json(collectedBulbasaur, { status: 201 })
      }),
    )
    const user = userEvent.setup()
    renderRoute('/pokemon/bulbasaur')

    await user.click(await screen.findByRole('button', { name: 'Add to collection' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Added to the collection')
    expect(authorization).toBe(bearer)
  })

  it('shows a visitor the collected Pokémon without edit, refresh or remove', async () => {
    serveBulbasaur()
    renderRoute(`/collection/${collectedBulbasaur.id}`)

    expect(await screen.findByRole('heading', { level: 1, name: 'Bulby' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Edit custom fields' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Refresh from PokeAPI' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Remove from collection' })).not.toBeInTheDocument()
  })

  it('lets a regular user edit and refresh but not remove', async () => {
    serveBulbasaur()
    startLoggedInAs(ash)
    renderRoute(`/collection/${collectedBulbasaur.id}`)

    expect(await screen.findByRole('button', { name: 'Edit custom fields' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Refresh from PokeAPI' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Remove from collection' })).not.toBeInTheDocument()
  })

  it('lets an admin remove', async () => {
    serveBulbasaur()
    startLoggedInAs(oak)
    renderRoute(`/collection/${collectedBulbasaur.id}`)

    expect(await screen.findByRole('button', { name: 'Remove from collection' })).toBeInTheDocument()
  })

  it('sends the user to log in again when the session has expired on the server', async () => {
    serveBulbasaur()
    startLoggedInAs(ash)
    server.use(
      http.post('*/api/v1/pokemon/sync', () =>
        HttpResponse.json({ status: 401, title: 'Authentication required' }, { status: 401 }),
      ),
    )
    const user = userEvent.setup()
    const { router } = renderRoute('/pokemon/bulbasaur')

    await user.click(await screen.findByRole('button', { name: 'Add to collection' }))

    expect(await screen.findByText('Your session expired. Log in again.')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/login')
    expect(sessionStorage.getItem('pokedex.session')).toBeNull()
  })

  it('closes an open edit form when the user logs out', async () => {
    serveBulbasaur()
    startLoggedInAs(ash)
    const user = userEvent.setup()
    renderRoute(`/collection/${collectedBulbasaur.id}`)

    await user.click(await screen.findByRole('button', { name: 'Edit custom fields' }))
    await user.click(screen.getByRole('button', { name: 'Log out' }))

    expect(screen.queryByRole('button', { name: 'Save' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Edit custom fields' })).not.toBeInTheDocument()
  })
})
