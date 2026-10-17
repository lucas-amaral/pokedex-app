import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { renderRoute } from '../../test/renderRoute'
import { server } from '../../test/server'
import { collectedBulbasaur, collectedCharmander } from './fixtures'

const ONE = '*/api/v1/pokemon/:id'

describe('CollectedPokemonPage', () => {
  it('shows the proprietary fields next to the PokeAPI data', async () => {
    server.use(http.get(ONE, () => HttpResponse.json(collectedBulbasaur)))
    renderRoute(`/collection/${collectedBulbasaur.id}`)

    expect(await screen.findByRole('heading', { level: 1, name: 'Bulby' })).toBeInTheDocument()
    expect(screen.getByText('Bulbasaur · #001')).toBeInTheDocument()
    const fields = screen.getByRole('region', { name: 'Your fields' })
    expect(fields).toHaveTextContent('Our first starter')
    expect(fields).toHaveTextContent('Kanto')
    expect(fields).toHaveTextContent('pt-BR')
    expect(fields).toHaveTextContent('Bulbassauro')
    expect(within(fields).getByRole('list', { name: 'Tags' })).toHaveTextContent('starter')
    expect(screen.getByRole('link', { name: 'View in catalog' })).toHaveAttribute('href', '/pokemon/bulbasaur')
  })

  it('uses the Pokémon name as title when there is no nickname', async () => {
    server.use(http.get(ONE, () => HttpResponse.json(collectedCharmander)))
    renderRoute(`/collection/${collectedCharmander.id}`)

    expect(await screen.findByRole('heading', { level: 1, name: 'Charmander' })).toBeInTheDocument()
    expect(screen.getByRole('region', { name: 'Your fields' })).toHaveTextContent('No nickname, notes, region, names or tags yet.')
  })

  it('refreshes the PokeAPI data on request', async () => {
    let synced: unknown
    server.use(
      http.get(ONE, () => HttpResponse.json(collectedBulbasaur)),
      http.post('*/api/v1/pokemon/sync', async ({ request }) => {
        synced = await request.json()
        return HttpResponse.json({ ...collectedBulbasaur, weightKg: 7 }, { status: 200 })
      }),
    )
    const user = userEvent.setup()
    renderRoute(`/collection/${collectedBulbasaur.id}`)

    await user.click(await screen.findByRole('button', { name: 'Refresh from PokeAPI' }))

    expect(await screen.findByRole('status')).toHaveTextContent('PokeAPI data refreshed')
    expect(synced).toEqual({ idOrName: '1' })
    expect(screen.getByText('7 kg')).toBeInTheDocument()
  })

  it('explains when the Pokémon is not in the collection', async () => {
    server.use(
      http.get(ONE, () =>
        HttpResponse.json({ status: 404, title: 'Pokémon not in the collection' }, { status: 404 }),
      ),
    )
    renderRoute('/collection/6f1c2a3b-0000-4000-8000-0000000000ff')

    expect(await screen.findByRole('heading', { name: 'Pokémon not in your collection' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Back to your collection' })).toHaveAttribute('href', '/collection')
  })
})
