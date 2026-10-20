import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { renderRoute } from '../../test/renderRoute'
import { server } from '../../test/server'
import { collectedBulbasaur, collectedCharmander, collectionPage } from './fixtures'

const COLLECTION = '*/api/v1/pokemon'

/** Answers like the service: filters by name/nickname and tag, and records every request. */
function serveCollection() {
  const requests: URL[] = []
  server.use(
    http.get(COLLECTION, ({ request }) => {
      const url = new URL(request.url)
      requests.push(url)
      const name = url.searchParams.get('name')
      const tag = url.searchParams.get('tag')
      const matching = [collectedBulbasaur, collectedCharmander]
        .filter((pokemon) => !name || pokemon.name.includes(name) || pokemon.nickname?.toLowerCase().includes(name))
        .filter((pokemon) => !tag || pokemon.tags.includes(tag))
      return HttpResponse.json(collectionPage(matching))
    }),
  )
  return requests
}

describe('CollectionPage', () => {
  it('lists the collected Pokémon with their nickname and tags', async () => {
    serveCollection()
    renderRoute('/collection')

    const bulbasaur = await screen.findByRole('link', { name: /Bulbasaur/ })
    expect(bulbasaur).toHaveAttribute('href', `/collection/${collectedBulbasaur.id}`)
    expect(bulbasaur).toHaveTextContent('Bulby')
    expect(bulbasaur.closest('li')).toHaveTextContent('starter')
    expect(screen.getByRole('link', { name: /Charmander/ })).toBeInTheDocument()
    expect(screen.getByText('2 Pokémon')).toBeInTheDocument()
  })

  it('filters by name and keeps the filter in the address', async () => {
    const requests = serveCollection()
    const user = userEvent.setup()
    const { router } = renderRoute('/collection')
    await screen.findByRole('link', { name: /Charmander/ })

    await user.type(screen.getByRole('searchbox', { name: 'Name, nickname or localized name' }), 'bulb{Enter}')

    expect(await screen.findByText('1 Pokémon')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /Charmander/ })).not.toBeInTheDocument()
    expect(router.state.location.search).toBe('?name=bulb')
    expect(requests.at(-1)?.searchParams.get('name')).toBe('bulb')
  })

  it('filters by a tag picked from a row', async () => {
    serveCollection()
    const user = userEvent.setup()
    const { router } = renderRoute('/collection')

    await user.click(await screen.findByRole('button', { name: 'Show Pokémon tagged starter' }))

    expect(await screen.findByText('1 Pokémon')).toBeInTheDocument()
    expect(router.state.location.search).toBe('?tag=starter')
    expect(screen.getByRole('button', { name: 'Clear filters' })).toBeInTheDocument()
  })

  it('opens with the filters given in the address', async () => {
    serveCollection()
    renderRoute('/collection?tag=starter')

    expect(await screen.findByText('1 Pokémon')).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'Tag' })).toHaveValue('starter')
  })

  it('invites the user to add Pokémon when the collection is empty', async () => {
    server.use(http.get(COLLECTION, () => HttpResponse.json(collectionPage([]))))
    renderRoute('/collection')

    const empty = await screen.findByRole('heading', { name: 'The collection is empty' })
    expect(empty).toBeInTheDocument()
    expect(within(empty.parentElement!).getByRole('link', { name: 'Browse the catalog' })).toHaveAttribute('href', '/')
  })

  it('says when no Pokémon match the filters', async () => {
    serveCollection()
    renderRoute('/collection?name=mewtwo')

    expect(await screen.findByText('No Pokémon match these filters.')).toBeInTheDocument()
  })
})
