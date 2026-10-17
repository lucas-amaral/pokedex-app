import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../../test/server'
import { fetchCollectionPage, syncPokemon } from './collectionApi'
import { collectedBulbasaur, collectionPage } from './fixtures'

describe('syncPokemon', () => {
  it('sends the number or name and reports a new addition', async () => {
    let body: unknown
    server.use(
      http.post('*/api/v1/pokemon/sync', async ({ request }) => {
        body = await request.json()
        return HttpResponse.json(collectedBulbasaur, { status: 201 })
      }),
    )

    const result = await syncPokemon('bulbasaur')

    expect(body).toEqual({ idOrName: 'bulbasaur' })
    expect(result).toEqual({ pokemon: collectedBulbasaur, created: true })
  })

  it('reports a refresh when the Pokémon was already in the collection', async () => {
    server.use(http.post('*/api/v1/pokemon/sync', () => HttpResponse.json(collectedBulbasaur, { status: 200 })))

    expect((await syncPokemon('bulbasaur')).created).toBe(false)
  })

  it('turns a problem into an ApiError', async () => {
    server.use(
      http.post('*/api/v1/pokemon/sync', () =>
        HttpResponse.json({ status: 404, title: 'Pokémon not found' }, { status: 404 }),
      ),
    )

    await expect(syncPokemon('missingno')).rejects.toMatchObject({ status: 404, title: 'Pokémon not found' })
  })
})

describe('fetchCollectionPage', () => {
  it('sends only the filters that are set', async () => {
    let requested: URL | undefined
    server.use(
      http.get('*/api/v1/pokemon', ({ request }) => {
        requested = new URL(request.url)
        return HttpResponse.json(collectionPage([collectedBulbasaur]))
      }),
    )

    await fetchCollectionPage({ page: 1, size: 20, name: 'bulb', tag: '' })

    expect(Object.fromEntries(requested!.searchParams)).toEqual({ page: '1', size: '20', name: 'bulb' })
  })
})
