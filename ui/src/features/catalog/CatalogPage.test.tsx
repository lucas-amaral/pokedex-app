import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { renderRoute } from '../../test/renderRoute'
import { server } from '../../test/server'
import { bulbasaur, catalogPage, ivysaur } from './fixtures'

const CATALOG = '*/api/v1/catalog/pokemon'

function respondWithPages() {
  server.use(
    http.get(CATALOG, ({ request }) => {
      const page = Number(new URL(request.url).searchParams.get('page'))
      return HttpResponse.json(catalogPage(page === 0 ? [bulbasaur] : [ivysaur], page, 20, 1351))
    }),
  )
}

function renderPage(path = '/') {
  return renderRoute(path)
}

describe('CatalogPage', () => {
  it('shows a loading state, then the first page of Pokémon', async () => {
    respondWithPages()
    renderPage()

    expect(screen.getByRole('status')).toHaveTextContent('Loading Pokémon')
    expect(await screen.findByRole('article', { name: 'Bulbasaur' })).toBeInTheDocument()
    expect(screen.getByText('Showing 1–1 of 1351')).toBeInTheDocument()
  })

  it('moves to the next page and back', async () => {
    respondWithPages()
    const user = userEvent.setup()
    const { router } = renderPage()
    await screen.findByRole('article', { name: 'Bulbasaur' })

    await user.click(screen.getByRole('button', { name: 'Next page' }))

    expect(await screen.findByRole('article', { name: 'Ivysaur' })).toBeInTheDocument()
    expect(screen.getByText('Page 2 of 68')).toBeInTheDocument()
    expect(router.state.location.search).toBe('?page=2')

    await user.click(screen.getByRole('button', { name: 'Previous page' }))

    expect(await screen.findByRole('article', { name: 'Bulbasaur' })).toBeInTheDocument()
  })

  it('opens the page given in the address, so a page can be shared or returned to', async () => {
    respondWithPages()
    renderPage('/?page=2')

    expect(await screen.findByRole('article', { name: 'Ivysaur' })).toBeInTheDocument()
  })

  it('falls back to the first page when the address has an invalid page', async () => {
    respondWithPages()
    renderPage('/?page=abc')

    expect(await screen.findByRole('article', { name: 'Bulbasaur' })).toBeInTheDocument()
  })

  it('does not offer a previous page on the first page', async () => {
    respondWithPages()
    renderPage()
    await screen.findByRole('article', { name: 'Bulbasaur' })

    expect(screen.getByRole('button', { name: 'Previous page' })).toBeDisabled()
  })

  it('explains the error from the service and lets the user retry', async () => {
    let calls = 0
    server.use(
      http.get(CATALOG, () => {
        calls += 1
        return calls === 1
          ? HttpResponse.json(
              { status: 503, title: 'Catalog unavailable', detail: 'The Pokémon catalog is not responding. Try again in a moment.' },
              { status: 503 },
            )
          : HttpResponse.json(catalogPage([bulbasaur]))
      }),
    )
    const user = userEvent.setup()
    renderPage()

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Catalog unavailable')
    expect(alert).toHaveTextContent('The Pokémon catalog is not responding. Try again in a moment.')

    await user.click(screen.getByRole('button', { name: 'Try again' }))

    expect(await screen.findByRole('article', { name: 'Bulbasaur' })).toBeInTheDocument()
  })
})
