import type { AuthUser } from '../features/auth/types'
import { loginResponse } from '../features/auth/fixtures'

/** Starts a test already logged in, the way a reload finds a stored session. */
export function startLoggedInAs(user: AuthUser) {
  const response = loginResponse(user)
  sessionStorage.setItem(
    'pokedex.session',
    JSON.stringify({ token: response.accessToken, expiresAt: response.expiresAt, user: response.user }),
  )
  return `Bearer ${response.accessToken}`
}
