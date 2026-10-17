import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { ApiError } from '../../api/ApiError'
import { server } from '../../test/server'
import { fetchCatalogPage } from './catalogApi'
import { bulbasaur, catalogPage } from './fixtures'

describe('fetchCatalogPage', () => {
  it('asks the service for the requested page and size', async () => {
    let requested: URL | undefined
    server.use(
      http.get('*/api/v1/catalog/pokemon', ({ request }) => {
        requested = new URL(request.url)
        return HttpResponse.json(catalogPage([bulbasaur], 3, 12))
      }),
    )

    const page = await fetchCatalogPage({ page: 3, size: 12 })

    expect(requested?.searchParams.get('page')).toBe('3')
    expect(requested?.searchParams.get('size')).toBe('12')
    expect(page.content[0].name).toBe('bulbasaur')
  })

  it('turns an RFC 9457 problem into an ApiError the UI can show', async () => {
    server.use(
      http.get('*/api/v1/catalog/pokemon', () =>
        HttpResponse.json(
          { status: 503, title: 'Catalog unavailable', detail: 'The Pokémon catalog is not responding. Try again in a moment.' },
          { status: 503, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    )

    const failure = fetchCatalogPage({ page: 0, size: 20 })

    await expect(failure).rejects.toBeInstanceOf(ApiError)
    await expect(failure).rejects.toMatchObject({
      status: 503,
      title: 'Catalog unavailable',
      detail: 'The Pokémon catalog is not responding. Try again in a moment.',
    })
  })

  it('still reports a readable error when the body is not a problem', async () => {
    server.use(http.get('*/api/v1/catalog/pokemon', () => new HttpResponse('Bad gateway', { status: 502 })))

    await expect(fetchCatalogPage({ page: 0, size: 20 })).rejects.toMatchObject({
      status: 502,
      title: 'Request failed',
    })
  })
})
