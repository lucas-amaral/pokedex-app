import { renderHook, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../../test/server'
import { createQueryWrapper } from '../../test/queryClient'
import { bulbasaur, catalogPage, ivysaur } from './fixtures'
import { useCatalogPage } from './useCatalogPage'

describe('useCatalogPage', () => {
  it('loads the requested page', async () => {
    server.use(http.get('*/api/v1/catalog/pokemon', () => HttpResponse.json(catalogPage([bulbasaur, ivysaur]))))

    const { result } = renderHook(() => useCatalogPage({ page: 0, size: 20 }), { wrapper: createQueryWrapper() })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data?.content.map((pokemon) => pokemon.name)).toEqual(['bulbasaur', 'ivysaur'])
  })

  it('keeps showing the previous page while the next one loads', async () => {
    server.use(
      http.get('*/api/v1/catalog/pokemon', ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get('page'))
        return HttpResponse.json(catalogPage(page === 0 ? [bulbasaur] : [ivysaur], page))
      }),
    )
    const { result, rerender } = renderHook(({ page }) => useCatalogPage({ page, size: 20 }), {
      wrapper: createQueryWrapper(),
      initialProps: { page: 0 },
    })
    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    rerender({ page: 1 })

    expect(result.current.data?.content[0].name).toBe('bulbasaur')
    expect(result.current.isPlaceholderData).toBe(true)
    await waitFor(() => expect(result.current.data?.content[0].name).toBe('ivysaur'))
  })

  it('exposes the API error', async () => {
    server.use(
      http.get('*/api/v1/catalog/pokemon', () =>
        HttpResponse.json({ status: 503, title: 'Catalog unavailable' }, { status: 503 }),
      ),
    )

    const { result } = renderHook(() => useCatalogPage({ page: 0, size: 20 }), { wrapper: createQueryWrapper() })

    await waitFor(() => expect(result.current.isError).toBe(true))
    expect(result.current.error?.title).toBe('Catalog unavailable')
  })
})
