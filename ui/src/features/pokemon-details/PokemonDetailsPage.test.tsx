import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { renderRoute } from '../../test/renderRoute'
import { server } from '../../test/server'
import { startLoggedInAs } from '../../test/session'
import { ash } from '../auth/fixtures'
import { collectedBulbasaur, collectionPage } from '../collection/fixtures'
import { alolanRaichuDetails, bulbasaurDetails, eeveeDetails } from './fixtures'

const DETAILS = '*/api/v1/catalog/pokemon/:idOrName'

function respondWith(...pokemon: (typeof bulbasaurDetails)[]) {
  server.use(
    http.get('*/api/v1/pokemon', () => HttpResponse.json(collectionPage([]))),
    http.get(DETAILS, ({ params }) => {
      const found = pokemon.find((candidate) => candidate.name === params.idOrName)
      return found
        ? HttpResponse.json(found)
        : HttpResponse.json(
            { status: 404, title: 'Pokémon not found', detail: `No Pokémon found for '${String(params.idOrName)}'` },
            { status: 404 },
          )
    }),
  )
}

describe('PokemonDetailsPage', () => {
  it('shows the artwork, identity and profile of the Pokémon', async () => {
    respondWith(bulbasaurDetails)
    renderRoute('/pokemon/bulbasaur')

    expect(await screen.findByRole('heading', { level: 1, name: 'Bulbasaur' })).toBeInTheDocument()
    expect(screen.getByRole('img', { name: 'Bulbasaur' })).toHaveAttribute('src', bulbasaurDetails.artworkUrl)
    expect(screen.getByText('#001')).toBeInTheDocument()
    expect(screen.getByText('Seed Pokémon')).toBeInTheDocument()
    expect(screen.getByText(bulbasaurDetails.description!)).toBeInTheDocument()
    expect(within(screen.getByRole('list', { name: 'Types' })).getAllByRole('listitem').map((item) => item.textContent)).toEqual([
      'Grass',
      'Poison',
    ])
    expect(screen.getByText('0.7 m')).toBeInTheDocument()
    expect(screen.getByText('6.9 kg')).toBeInTheDocument()
  })

  it('shows each base stat as a bar and their total', async () => {
    respondWith(bulbasaurDetails)
    renderRoute('/pokemon/bulbasaur')

    expect(await screen.findByRole('meter', { name: 'HP' })).toHaveAttribute('aria-valuenow', '45')
    expect(screen.getByRole('meter', { name: 'Sp. Atk' })).toHaveAttribute('aria-valuenow', '65')
    expect(screen.getByText('318')).toBeInTheDocument()
  })

  it('shows the evolution chain with how each stage is reached', async () => {
    respondWith(bulbasaurDetails)
    renderRoute('/pokemon/bulbasaur')

    const chain = await screen.findByRole('region', { name: 'Evolution chain' })
    expect(within(chain).getByRole('link', { name: /Bulbasaur/ })).toHaveAttribute('aria-current', 'page')
    expect(within(chain).getByRole('link', { name: /Ivysaur/ })).toHaveAttribute('href', '/pokemon/ivysaur')
    expect(chain).toHaveTextContent('Lv. 16')
    expect(chain).toHaveTextContent('Lv. 32')
  })

  it('highlights the species of an alternate form in the chain', async () => {
    respondWith(alolanRaichuDetails)
    renderRoute('/pokemon/raichu-alola')

    const chain = await screen.findByRole('region', { name: 'Evolution chain' })
    expect(within(chain).getByRole('link', { name: /Raichu/ })).toHaveAttribute('aria-current', 'page')
    expect(within(chain).getByRole('link', { name: /Pikachu/ })).not.toHaveAttribute('aria-current')
  })

  it('names the browser tab after the Pokémon', async () => {
    respondWith(bulbasaurDetails)
    renderRoute('/pokemon/bulbasaur')

    await waitFor(() => expect(document.title).toBe('Bulbasaur · Pokédex'))
  })

  it('treats a malformed address as an unknown Pokémon without calling the service', async () => {
    renderRoute('/pokemon/mew!')

    expect(await screen.findByRole('heading', { name: 'Pokémon not found' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Try again' })).not.toBeInTheDocument()
  })

  it('offers to add a Pokémon that is not in the collection yet', async () => {
    startLoggedInAs(ash)
    let lookedUp: URL | undefined
    respondWith(bulbasaurDetails)
    server.use(
      http.get('*/api/v1/pokemon', ({ request }) => {
        lookedUp = new URL(request.url)
        return HttpResponse.json(collectionPage([]))
      }),
    )
    renderRoute('/pokemon/bulbasaur')

    expect(await screen.findByRole('button', { name: 'Add to collection' })).toBeInTheDocument()
    expect(lookedUp?.searchParams.get('pokeapiId')).toBe('1')
  })

  it('adds the Pokémon, then shows it as collected instead of the button', async () => {
    startLoggedInAs(ash)
    respondWith(bulbasaurDetails)
    let collected = false
    server.use(
      http.get('*/api/v1/pokemon', () => HttpResponse.json(collectionPage(collected ? [collectedBulbasaur] : []))),
      http.post('*/api/v1/pokemon/sync', () => {
        collected = true
        return HttpResponse.json(collectedBulbasaur, { status: 201 })
      }),
    )
    const user = userEvent.setup()
    renderRoute('/pokemon/bulbasaur')

    await user.click(await screen.findByRole('button', { name: 'Add to collection' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Added to your collection')
    expect(screen.getByRole('link', { name: 'Open in your collection' })).toHaveAttribute(
      'href',
      `/collection/${collectedBulbasaur.id}`,
    )
    expect(screen.queryByRole('button', { name: 'Add to collection' })).not.toBeInTheDocument()
  })

  it('starts fresh for the next Pokémon after adding one', async () => {
    startLoggedInAs(ash)
    respondWith(bulbasaurDetails, { ...bulbasaurDetails, id: 2, name: 'ivysaur', speciesName: 'ivysaur' })
    server.use(http.post('*/api/v1/pokemon/sync', () => HttpResponse.json(collectedBulbasaur, { status: 201 })))
    const user = userEvent.setup()
    renderRoute('/pokemon/bulbasaur')
    await user.click(await screen.findByRole('button', { name: 'Add to collection' }))
    await screen.findByRole('status')

    const chain = screen.getByRole('region', { name: 'Evolution chain' })
    await user.click(within(chain).getByRole('link', { name: /Ivysaur/ }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Ivysaur' })).toBeInTheDocument()
    expect(await screen.findByRole('button', { name: 'Add to collection' })).toBeInTheDocument()
    expect(screen.queryByText(/Added to your collection/)).not.toBeInTheDocument()
  })

  it('does not offer to add a Pokémon that is already collected', async () => {
    startLoggedInAs(ash)
    respondWith(bulbasaurDetails)
    server.use(http.get('*/api/v1/pokemon', () => HttpResponse.json(collectionPage([collectedBulbasaur]))))
    renderRoute('/pokemon/bulbasaur')

    expect(await screen.findByText('In your collection')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Open in your collection' })).toHaveAttribute(
      'href',
      `/collection/${collectedBulbasaur.id}`,
    )
    expect(screen.queryByRole('button', { name: 'Add to collection' })).not.toBeInTheDocument()
  })

  it('explains when adding to the collection fails', async () => {
    startLoggedInAs(ash)
    respondWith(bulbasaurDetails)
    server.use(
      http.post('*/api/v1/pokemon/sync', () =>
        HttpResponse.json({ status: 503, title: 'Catalog unavailable', detail: 'Try again in a moment.' }, { status: 503 }),
      ),
    )
    const user = userEvent.setup()
    renderRoute('/pokemon/bulbasaur')

    await user.click(await screen.findByRole('button', { name: 'Add to collection' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Catalog unavailable')
  })

  it('shows every branch of a branching chain', async () => {
    respondWith(eeveeDetails)
    renderRoute('/pokemon/eevee')

    const chain = await screen.findByRole('region', { name: 'Evolution chain' })
    expect(within(chain).getAllByRole('link')).toHaveLength(4)
    expect(chain).toHaveTextContent('Water Stone')
    expect(chain).toHaveTextContent('Thunder Stone')
  })

  it('opens another stage of the chain', async () => {
    respondWith(bulbasaurDetails, { ...bulbasaurDetails, id: 2, name: 'ivysaur' })
    const user = userEvent.setup()
    const { router } = renderRoute('/pokemon/bulbasaur')

    const chain = await screen.findByRole('region', { name: 'Evolution chain' })
    await user.click(within(chain).getByRole('link', { name: /Ivysaur/ }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Ivysaur' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/pokemon/ivysaur')
  })

  it('explains when the Pokémon does not exist', async () => {
    respondWith()
    renderRoute('/pokemon/missingno')

    expect(await screen.findByRole('heading', { name: 'Pokémon not found' })).toBeInTheDocument()
    expect(screen.getByText("No Pokémon found for 'missingno'")).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Back to the catalog' })).toHaveAttribute('href', '/')
  })

  it('lets the user retry when the service fails', async () => {
    let calls = 0
    server.use(
      http.get(DETAILS, () => {
        calls += 1
        return calls === 1
          ? HttpResponse.json({ status: 503, title: 'Catalog unavailable' }, { status: 503 })
          : HttpResponse.json(bulbasaurDetails)
      }),
    )
    const user = userEvent.setup()
    renderRoute('/pokemon/bulbasaur')

    await user.click(await screen.findByRole('button', { name: 'Try again' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Bulbasaur' })).toBeInTheDocument()
  })
})
