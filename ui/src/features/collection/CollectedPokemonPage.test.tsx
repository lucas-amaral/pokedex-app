import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { renderRoute } from '../../test/renderRoute'
import { server } from '../../test/server'
import { startLoggedInAs } from '../../test/session'
import { ash, oak } from '../auth/fixtures'
import { collectedBulbasaur, collectedCharmander, collectionPage } from './fixtures'

const ONE = '*/api/v1/pokemon/:id'

describe('CollectedPokemonPage', () => {
  it('shows the proprietary fields next to the PokeAPI data', async () => {
    server.use(http.get(ONE, () => HttpResponse.json(collectedBulbasaur)))
    renderRoute(`/collection/${collectedBulbasaur.id}`)

    expect(await screen.findByRole('heading', { level: 1, name: 'Bulby' })).toBeInTheDocument()
    expect(screen.getByText('Bulbasaur · #001')).toBeInTheDocument()
    const fields = screen.getByRole('region', { name: 'Custom fields' })
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
    expect(screen.getByRole('region', { name: 'Custom fields' })).toHaveTextContent('No nickname, notes, region, names or tags yet.')
  })

  it('refreshes the PokeAPI data on request', async () => {
    startLoggedInAs(ash)
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

    expect(await screen.findByRole('heading', { name: 'Pokémon not in the collection' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Back to the collection' })).toHaveAttribute('href', '/collection')
  })
  it('edits the proprietary fields and shows the saved values', async () => {
    startLoggedInAs(ash)
    let sent: unknown
    server.use(
      http.get(ONE, () => HttpResponse.json(collectedBulbasaur)),
      http.put(ONE, async ({ request }) => {
        sent = await request.json()
        return HttpResponse.json({ ...collectedBulbasaur, nickname: 'Bulbie', tags: ['grass-team', 'starter'], version: 1 })
      }),
    )
    const user = userEvent.setup()
    renderRoute(`/collection/${collectedBulbasaur.id}`)

    await user.click(await screen.findByRole('button', { name: 'Edit custom fields' }))
    const nickname = screen.getByRole('textbox', { name: 'Nickname' })
    expect(nickname).toHaveValue('Bulby')
    await user.clear(nickname)
    await user.type(nickname, 'Bulbie')
    await user.type(screen.getByRole('textbox', { name: 'Tags' }), ', Grass-Team')
    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Saved')
    expect(screen.getByRole('heading', { level: 1, name: 'Bulbie' })).toBeInTheDocument()
    expect(sent).toEqual({
      version: 0,
      nickname: 'Bulbie',
      notes: 'Our first starter',
      region: 'Kanto',
      localizedNames: { ja: 'フシギダネ', 'pt-BR': 'Bulbassauro' },
      tags: ['starter', 'grass-team'],
    })
  })

  it('adds and removes localized names', async () => {
    startLoggedInAs(ash)
    let sent: { localizedNames?: Record<string, string> } = {}
    server.use(
      http.get(ONE, () => HttpResponse.json(collectedCharmander)),
      http.put(ONE, async ({ request }) => {
        sent = (await request.json()) as typeof sent
        return HttpResponse.json({ ...collectedCharmander, version: 1 })
      }),
    )
    const user = userEvent.setup()
    renderRoute(`/collection/${collectedCharmander.id}`)

    await user.click(await screen.findByRole('button', { name: 'Edit custom fields' }))
    await user.click(screen.getByRole('button', { name: 'Add a language' }))
    await user.type(screen.getByRole('textbox', { name: 'Language 1' }), 'pt-BR')
    await user.type(screen.getByRole('textbox', { name: 'Name in language 1' }), 'Charmander')
    await user.click(screen.getByRole('button', { name: 'Add a language' }))
    await user.click(screen.getByRole('button', { name: 'Remove language 2' }))
    await user.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() => expect(sent.localizedNames).toEqual({ 'pt-BR': 'Charmander' }))
  })

  it('explains invalid input before sending anything', async () => {
    startLoggedInAs(ash)
    server.use(http.get(ONE, () => HttpResponse.json(collectedBulbasaur)))
    const user = userEvent.setup()
    renderRoute(`/collection/${collectedBulbasaur.id}`)

    await user.click(await screen.findByRole('button', { name: 'Edit custom fields' }))
    const tags = screen.getByRole('textbox', { name: 'Tags' })
    await user.clear(tags)
    await user.type(tags, 'has space')
    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByText(/lowercase letters, digits and hyphens/)).toBeInTheDocument()
    expect(tags).toHaveAttribute('aria-invalid', 'true')
  })

  it('shows the errors the service finds next to the fields', async () => {
    startLoggedInAs(ash)
    server.use(
      http.get(ONE, () => HttpResponse.json(collectedBulbasaur)),
      http.put(ONE, () =>
        HttpResponse.json(
          { status: 400, title: 'Invalid request', errors: [{ field: 'region', message: 'region must be at most 50 characters' }] },
          { status: 400 },
        ),
      ),
    )
    const user = userEvent.setup()
    renderRoute(`/collection/${collectedBulbasaur.id}`)

    await user.click(await screen.findByRole('button', { name: 'Edit custom fields' }))
    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('region must be at most 50 characters')).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'Region' })).toHaveAttribute('aria-invalid', 'true')
  })

  it('explains a conflict and offers to load the latest version', async () => {
    startLoggedInAs(ash)
    let reads = 0
    server.use(
      http.get(ONE, () => {
        reads += 1
        return HttpResponse.json(reads === 1 ? collectedBulbasaur : { ...collectedBulbasaur, nickname: 'Seedy', version: 1 })
      }),
      http.put(ONE, () =>
        HttpResponse.json(
          { status: 409, title: 'Pokémon changed since you loaded it', detail: 'Someone else changed this Pokémon.', currentVersion: 1 },
          { status: 409 },
        ),
      ),
    )
    const user = userEvent.setup()
    renderRoute(`/collection/${collectedBulbasaur.id}`)

    await user.click(await screen.findByRole('button', { name: 'Edit custom fields' }))
    await user.click(screen.getByRole('button', { name: 'Save' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Pokémon changed since you loaded it')

    await user.click(screen.getByRole('button', { name: 'Load the latest version' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Seedy' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Save' })).not.toBeInTheDocument()
  })

  it('cancels an edit without saving', async () => {
    startLoggedInAs(ash)
    server.use(http.get(ONE, () => HttpResponse.json(collectedBulbasaur)))
    const user = userEvent.setup()
    renderRoute(`/collection/${collectedBulbasaur.id}`)

    await user.click(await screen.findByRole('button', { name: 'Edit custom fields' }))
    await user.type(screen.getByRole('textbox', { name: 'Nickname' }), 'zzz')
    await user.click(screen.getByRole('button', { name: 'Cancel' }))

    expect(screen.getByRole('region', { name: 'Custom fields' })).toHaveTextContent('Bulby')
    expect(screen.getByRole('region', { name: 'Custom fields' })).not.toHaveTextContent('Bulbyzzz')
  })

  it('removes the Pokémon after confirmation and says so in the collection', async () => {
    startLoggedInAs(oak)
    let deleted = false
    server.use(
      http.get(ONE, () => HttpResponse.json(collectedBulbasaur)),
      http.delete(ONE, () => {
        deleted = true
        return new HttpResponse(null, { status: 204 })
      }),
      http.get('*/api/v1/pokemon', () => HttpResponse.json(collectionPage([collectedCharmander]))),
    )
    const user = userEvent.setup()
    const { router } = renderRoute(`/collection/${collectedBulbasaur.id}`)

    await user.click(await screen.findByRole('button', { name: 'Remove from collection' }))
    expect(screen.getByRole('alertdialog')).toHaveTextContent('Remove Bulby from the collection?')
    await user.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Remove' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Bulby was removed from the collection.')
    expect(router.state.location.pathname).toBe('/collection')
    expect(deleted).toBe(true)
  })

  it('keeps the Pokémon when the removal is cancelled', async () => {
    startLoggedInAs(oak)
    server.use(http.get(ONE, () => HttpResponse.json(collectedBulbasaur)))
    const user = userEvent.setup()
    renderRoute(`/collection/${collectedBulbasaur.id}`)

    await user.click(await screen.findByRole('button', { name: 'Remove from collection' }))
    await user.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Cancel' }))

    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1, name: 'Bulby' })).toBeInTheDocument()
  })
})
