import type { Session } from './types'

const KEY = 'pokedex.session'

/**
 * The session lives in sessionStorage: it survives a reload of the tab and is gone when the tab closes.
 * An expired or unreadable session is treated as no session.
 */
export function readSession(now = Date.now()): Session | null {
  try {
    const stored = sessionStorage.getItem(KEY)
    if (!stored) return null
    const session = JSON.parse(stored) as Session
    if (!session.token || Date.parse(session.expiresAt) <= now) {
      sessionStorage.removeItem(KEY)
      return null
    }
    return session
  } catch {
    return null
  }
}

export function saveSession(session: Session) {
  try {
    sessionStorage.setItem(KEY, JSON.stringify(session))
  } catch {
    // Storage can be unavailable (private mode); the session then lasts until the page reloads.
  }
}

export function clearSession() {
  try {
    sessionStorage.removeItem(KEY)
  } catch {
    // Nothing to clear.
  }
}
