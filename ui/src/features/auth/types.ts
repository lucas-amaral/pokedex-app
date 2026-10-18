/** Mirrors the service's UserResponse. */
export interface AuthUser {
  id: string
  username: string
  email: string
  role: 'USER' | 'ADMIN'
  createdAt: string
}

/** Mirrors the service's LoginResponse. */
export interface LoginResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresAt: string
  user: AuthUser
}

/** What the browser keeps between reloads of the tab. */
export interface Session {
  token: string
  expiresAt: string
  user: AuthUser
}
