import { type ReactNode, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { onUnauthorized, setAccessToken } from '../../api/http'
import { AuthContext, type AuthState } from './authContext'
import { clearSession, readSession, saveSession } from './sessionStorage'
import type { LoginResponse, Session } from './types'

/**
 * Holds the logged-in user. The token is handed to the HTTP helper as soon as it is known (not in an effect),
 * so the very first requests after a reload already carry it. If the service rejects the token, the user is
 * logged out and sent to the login page, which explains that the session expired.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(() => {
    const stored = readSession()
    setAccessToken(stored?.token ?? null)
    return stored
  })
  const navigate = useNavigate()
  const location = useLocation()
  const currentPath = useRef(location)
  useEffect(() => {
    currentPath.current = location
  }, [location])

  const signIn = useCallback((response: LoginResponse) => {
    const next: Session = { token: response.accessToken, expiresAt: response.expiresAt, user: response.user }
    saveSession(next)
    setAccessToken(next.token)
    setSession(next)
  }, [])

  const signOut = useCallback(() => {
    clearSession()
    setAccessToken(null)
    setSession(null)
  }, [])

  useEffect(
    () =>
      onUnauthorized(() => {
        signOut()
        void navigate('/login', { state: { from: currentPath.current, expired: true } })
      }),
    [navigate, signOut],
  )

  const value = useMemo<AuthState>(
    () => ({ user: session?.user ?? null, isAdmin: session?.user.role === 'ADMIN', signIn, signOut }),
    [session, signIn, signOut],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
