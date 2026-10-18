import type { AuthUser, LoginResponse } from './types'

export const ash: AuthUser = {
  id: '9e1c2a3b-0000-4000-8000-0000000000a1',
  username: 'ash',
  email: 'ash@pallet.town',
  role: 'USER',
  createdAt: '2026-10-18T10:00:00Z',
}

export const oak: AuthUser = { ...ash, id: '9e1c2a3b-0000-4000-8000-0000000000b2', username: 'oak', email: 'oak@pallet.town', role: 'ADMIN' }

export function loginResponse(user: AuthUser, token = `token-of-${user.username}`): LoginResponse {
  return { accessToken: token, tokenType: 'Bearer', expiresAt: '2999-01-01T00:00:00Z', user }
}
