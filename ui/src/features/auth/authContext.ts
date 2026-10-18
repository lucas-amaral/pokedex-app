import { createContext, useContext } from 'react'
import type { AuthUser, LoginResponse } from './types'

export interface AuthState {
  user: AuthUser | null
  isAdmin: boolean
  signIn: (response: LoginResponse) => void
  signOut: () => void
}

export const AuthContext = createContext<AuthState | null>(null)

/** Who is logged in, and how to log in or out. */
export function useAuth(): AuthState {
  const auth = useContext(AuthContext)
  if (!auth) throw new Error('useAuth must be used inside AuthProvider')
  return auth
}
