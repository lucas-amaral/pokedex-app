import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { catalogPage } from '../catalog/fixtures'
import { collectionPage } from '../collection/fixtures'
import { renderRoute } from '../../test/renderRoute'
import { server } from '../../test/server'
import { startLoggedInAs } from '../../test/session'
import { ash, loginResponse, oak } from './fixtures'

describe('authentication', () => {
  beforeEach(() => {
    server.use(
      http.get('*/api/v1/catalog/pokemon', () => HttpResponse.json(catalogPage([]))),
      http.get('*/api/v1/pokemon', () => HttpResponse.json(collectionPage([]))),
    )
  })

  it('offers to log in or sign up when nobody is logged in', () => {
    renderRoute('/')

    expect(screen.getByRole('link', { name: 'Log in' })).toHaveAttribute('href', '/login')
    expect(screen.getByRole('link', { name: 'Sign up' })).toHaveAttribute('href', '/register')
  })

  it('logs in, keeps the session and returns to the page the user came from', async () => {
    let sent: unknown
    server.use(
      http.post('*/api/v1/auth/login', async ({ request }) => {
        sent = await request.json()
        return HttpResponse.json(loginResponse(ash))
      }),
    )
    const user = userEvent.setup()
    const { router } = renderRoute('/collection')

    await user.click(screen.getByRole('link', { name: 'Log in' }))
    await user.type(screen.getByRole('textbox', { name: 'Username' }), 'ash')
    await user.type(screen.getByLabelText('Password'), 'pikachu123')
    await user.click(screen.getByRole('button', { name: 'Log in' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/collection'))
    expect(sent).toEqual({ username: 'ash', password: 'pikachu123' })
    expect(screen.getByRole('banner')).toHaveTextContent('ash')
    expect(JSON.parse(sessionStorage.getItem('pokedex.session')!).token).toBe('token-of-ash')
  })

  it('explains wrong credentials without saying which one was wrong', async () => {
    server.use(
      http.post('*/api/v1/auth/login', () =>
        HttpResponse.json({ status: 401, title: 'Login failed', detail: 'Wrong username or password' }, { status: 401 }),
      ),
    )
    const user = userEvent.setup()
    renderRoute('/login')

    await user.type(screen.getByRole('textbox', { name: 'Username' }), 'ash')
    await user.type(screen.getByLabelText('Password'), 'not-the-password')
    await user.click(screen.getByRole('button', { name: 'Log in' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Wrong username or password')
  })

  it('asks for both fields before sending anything', async () => {
    const user = userEvent.setup()
    renderRoute('/login')

    await user.click(screen.getByRole('button', { name: 'Log in' }))

    expect(await screen.findByText('Enter your username')).toBeInTheDocument()
    expect(screen.getByText('Enter your password')).toBeInTheDocument()
  })

  it('signs up, then is logged in straight away', async () => {
    let registered: unknown
    server.use(
      http.post('*/api/v1/auth/register', async ({ request }) => {
        registered = await request.json()
        return HttpResponse.json(ash, { status: 201 })
      }),
      http.post('*/api/v1/auth/login', () => HttpResponse.json(loginResponse(ash))),
    )
    const user = userEvent.setup()
    const { router } = renderRoute('/register')

    await user.type(screen.getByRole('textbox', { name: 'Username' }), 'ash')
    await user.type(screen.getByRole('textbox', { name: 'Email' }), 'ash@pallet.town')
    await user.type(screen.getByLabelText('Password'), 'pikachu123')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/'))
    expect(registered).toEqual({ username: 'ash', email: 'ash@pallet.town', password: 'pikachu123' })
    expect(screen.getByRole('banner')).toHaveTextContent('ash')
  })

  it('checks the sign-up rules before sending anything', async () => {
    const user = userEvent.setup()
    renderRoute('/register')

    await user.type(screen.getByRole('textbox', { name: 'Username' }), 'a!')
    await user.type(screen.getByRole('textbox', { name: 'Email' }), 'not-an-email')
    await user.type(screen.getByLabelText('Password'), 'short')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByText(/3 to 30/)).toBeInTheDocument()
    expect(screen.getByText('Enter a valid email address')).toBeInTheDocument()
    expect(screen.getByText(/at least 8 characters/)).toBeInTheDocument()
  })

  it('shows a taken username next to the field', async () => {
    server.use(
      http.post('*/api/v1/auth/register', () =>
        HttpResponse.json({ status: 409, title: 'Username taken', detail: 'That username is already taken' }, { status: 409 }),
      ),
    )
    const user = userEvent.setup()
    renderRoute('/register')

    await user.type(screen.getByRole('textbox', { name: 'Username' }), 'ash')
    await user.type(screen.getByRole('textbox', { name: 'Email' }), 'ash@pallet.town')
    await user.type(screen.getByLabelText('Password'), 'pikachu123')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByText('That username is already taken')).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'Username' })).toHaveAttribute('aria-invalid', 'true')
  })

  it('logs out', async () => {
    startLoggedInAs(ash)
    const user = userEvent.setup()
    renderRoute('/')

    await user.click(screen.getByRole('button', { name: 'Log out' }))

    expect(screen.getByRole('link', { name: 'Log in' })).toBeInTheDocument()
    expect(sessionStorage.getItem('pokedex.session')).toBeNull()
  })

  it('marks an admin in the header', () => {
    startLoggedInAs(oak)
    renderRoute('/')

    expect(screen.getByRole('banner')).toHaveTextContent('oak')
    expect(screen.getByRole('banner')).toHaveTextContent('Admin')
  })

  it('forgets a stored session that has expired', () => {
    sessionStorage.setItem(
      'pokedex.session',
      JSON.stringify({ token: 'old', expiresAt: '2000-01-01T00:00:00Z', user: ash }),
    )
    renderRoute('/')

    expect(screen.getByRole('link', { name: 'Log in' })).toBeInTheDocument()
  })
})
